from fastapi import FastAPI
from pydantic import BaseModel, Field
from modelscope.pipelines import pipeline
from modelscope.utils.constant import Tasks
from typing import List, Optional
from fastapi.responses import JSONResponse
from starlette.status import (
    HTTP_200_OK,
    HTTP_400_BAD_REQUEST,
    HTTP_500_INTERNAL_SERVER_ERROR
)
import re
import traceback

app = FastAPI()

# 模型初始化
try:
    translation_pipeline = pipeline(task=Tasks.translation, model='damo/nlp_csanmt_translation_en2zh')
    print("Model initialized successfully.")
except Exception as e:
    print(f"Model initialization Error: {e}")
    translation_pipeline = None


# 翻译结果
class Translation(BaseModel):
    src: str
    dst: str

# 输出JSON
class TranslateResponse(BaseModel):
    from_lang: Optional[str] = Field(None, alias="from")
    to_lang: Optional[str] = Field(None, alias="to")
    trans_result: Optional[List[Translation]] = None
    error_code: Optional[str] = None
    error_msg: Optional[str] = None
    model_config = {
        "populate_by_name": True
    }


# 输入JSON
class TranslationRequest(BaseModel):
    text: str

# 分段
def split_text_by_line_keep_empty(text: str) -> List[str]:
    return text.splitlines()


async def perform_paragraph_translation(input_text: str):
    # 初始化翻译模型
    if translation_pipeline is None:
        return JSONResponse(
            status_code=HTTP_500_INTERNAL_SERVER_ERROR,
            content=TranslateResponse(
                error_code="50001",
                error_msg="Server Error: Translation model were not loaded."
            ).model_dump(by_alias=True)
        )

    # 分割段落
    lines_to_translate = split_text_by_line_keep_empty(input_text)

    # 如果没有可翻译的内容，返回错误
    if not lines_to_translate:
        return JSONResponse(
            status_code=HTTP_400_BAD_REQUEST,
            content=TranslateResponse(
                error_code="40004",
                error_msg="Input text is empty."
            ).model_dump(by_alias=True)
        )

    translated_segments: List[Translation] = []

    # 翻译每个段落
    for src_line in lines_to_translate:
        try:
            if src_line.strip() == "":
                continue
            result = translation_pipeline(src_line)
            translated_text = ""

            # 提取翻译结果
            if isinstance(result, dict) and 'text' in result:
                translated_text = result['text']
            elif isinstance(result, str):
                translated_text = result
            elif isinstance(result, dict) and 'translation' in result:
                translated_text = result['translation']
            else:
                print(f"Warning: Unexpected model. '{src_line[:50]}...': {result}")
                translated_segments.append(Translation(
                    src=src_line,
                    dst="Translation error: Unexpected output."
                ))
                continue
            translated_text = postprocess_punctuation(translated_text)
            translated_segments.append(Translation(src=src_line, dst=translated_text))

        except Exception as e:
            print(f"Error translating line '{src_line[:50]}...': {e}")
            traceback.print_exc()
            translated_segments.append(Translation(
                src=src_line,
                dst=f"Translation failed: {str(e)}"
            ))

    if not translated_segments:
        return JSONResponse(
            status_code=HTTP_500_INTERNAL_SERVER_ERROR,
            content=TranslateResponse(
                error_code="50005",
                error_msg="No segments were successfully translated."
            ).model_dump(by_alias=True)
        )

    return TranslateResponse(
        from_lang="en",
        to_lang="zh",
        trans_result=translated_segments
    )

# 替换标点符号 解决分段翻译后标点符号不对的问题
def postprocess_punctuation(text):
    text = re.sub(r'(?<=\w)\.', '。', text)
    text = re.sub(r'(?<=\w),', '，', text)
    text = re.sub(r'(?<=\w);', '；', text)
    text = re.sub(r'(?<=\w):', '：', text)
    text = re.sub(r'(?<=\w)\?', '？', text)
    text = re.sub(r'(?<=\w)!', '！', text)
    text = text.replace('(', '（').replace(')', '）')
    text = text.replace('[', '【').replace(']', '】')
    text = text.replace('"', '“')
    text = text.replace("'", '’')

    return text
@app.post(
    "/translate/en2zh",
    response_model=TranslateResponse,
    responses={
        HTTP_200_OK: {"model": TranslateResponse, "description": "Successful translation"},
        HTTP_400_BAD_REQUEST: {"model": TranslateResponse, "description": "Client error (e.g., bad input)"},
        HTTP_500_INTERNAL_SERVER_ERROR: {"model": TranslateResponse,
                                         "description": "Server error (e.g., model issue)"},
    }
)
async def translate_english_to_chinese_post(
        request: TranslationRequest
):
    return await perform_paragraph_translation(request.text)