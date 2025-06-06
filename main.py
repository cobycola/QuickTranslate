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
    translation_line = pipeline(task=Tasks.translation, model='damo/nlp_csanmt_translation_en2zh')
    print("Model initialized successfully.")
except Exception as e:
    print(f"Model initialization Error: {e}")
    translation_line = None


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
def split_text(text: str) -> List[str]:
    return text.splitlines()

def clean_text_spacing(text: str) -> str:
    text = re.sub(r'\s*([.!?;,:])\s*', r'\1', text.strip())
    text = re.sub(r'\s+', ' ', text)
    return text

async def doTranslation(input_text: str):
    # 初始化翻译模型
    if translation_line is None:
        return JSONResponse(
            status_code=HTTP_500_INTERNAL_SERVER_ERROR,
            content=TranslateResponse(
                error_code="50001",
                error_msg="Server Error: Translation model were not loaded."
            ).model_dump(by_alias=True)
        )

    # 分割段落
    lines = split_text(input_text)

    # 如果没有可翻译的内容，返回错误
    if not lines:
        return JSONResponse(
            status_code=HTTP_400_BAD_REQUEST,
            content=TranslateResponse(
                error_code="40004",
                error_msg="Error: Input text is empty."
            ).model_dump(by_alias=True)
        )

    results: List[Translation] = []

    # 翻译每个段落
    for src_line in lines:
        try:
            src_line = src_line.strip()
            if src_line.strip() == "":
                continue
            src_line = clean_text_spacing(src_line)
            print(f"正在翻译：{repr(src_line)}，长度：{len(src_line)}")
            result = translation_line(src_line)
            translation = ""
            # 提取翻译结果
            if isinstance(result, dict) and 'text' in result:
                translation = result['text']
            elif isinstance(result, str):
                translation = result
            elif isinstance(result, dict) and 'translation' in result:
                translation = result['translation']
            else:
                print(f"Warning: Unexpected model output format for line '{src_line[:50]}...': {result}")
                results.append(Translation(
                    src=src_line,
                    dst="Translation error: Unexpected output format."
                ))
                continue

            results.append(Translation(src=src_line, dst=translation))

        except Exception as e:
            print(f"Error translating line '{src_line[:50]}...': {e}")
            traceback.print_exc()
            results.append(Translation(
                src=src_line,
                dst=f"Translation failed: {str(e)}"
            ))

        if not results:
            return JSONResponse(
                status_code=HTTP_500_INTERNAL_SERVER_ERROR,
                content=TranslateResponse(
                    error_code="50005",
                    error_msg="No results were successfully translated."
                ).model_dump(by_alias=True)
            )

    return TranslateResponse(
        from_lang="en",
        to_lang="zh",
        trans_result=results
    )

def postprocess_punctuation(text):
    # 替换标点符号 解决分段翻译后标点符号不对的问题
    text = re.sub(r'(?<=\w)\.', '。', text)
    text = re.sub(r'(?<=\w),', '，', text)
    text = re.sub(r'(?<=\w);', '；', text)
    text = re.sub(r'(?<=\w):', '：', text)
    text = re.sub(r'(?<=\w)\?', '？', text)
    text = re.sub(r'(?<=\w)!', '！', text)
    text = text.replace('(', '(').replace(')', ')')
    text = text.replace('[', '【').replace(']', '】')
    text = text.replace('"', '“')
    text = text.replace("'", '’')
    # 清除标点前后多余空格
    text = re.sub(r'\s+([，。！？：；\-])', r'\1', text)  # 删除标点前空格
    text = re.sub(r'([，。！？：；\-])\s+', r'\1', text)  # 删除标点后空格
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
    return await doTranslation(request.text)