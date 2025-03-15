from flask import Flask, request, jsonify
from transformers import AutoProcessor, AutoModelForCausalLM
from PIL import Image
import torch
import pytesseract

model_id = 'microsoft/Florence-2-large-ft'
model = AutoModelForCausalLM.from_pretrained(model_id, trust_remote_code=True, torch_dtype='auto').eval().cuda()
processor = AutoProcessor.from_pretrained(model_id, trust_remote_code=True)

def run(task_prompt, text_input=None, image=None):
    if text_input is None:
        prompt = task_prompt
    else:
        prompt = task_prompt + text_input
    inputs = processor(text=prompt, images=image, return_tensors="pt").to('cuda', torch.float16)
    generated_ids = model.generate(
      input_ids=inputs["input_ids"].cuda(),
      pixel_values=inputs["pixel_values"].cuda(),
      max_new_tokens=1024,
      early_stopping=False,
      do_sample=False,
      num_beams=3,
    )
    generated_text = processor.batch_decode(generated_ids, skip_special_tokens=False)[0]
    parsed_answer = processor.post_process_generation(
        generated_text,
        task=task_prompt,
        image_size=(image.width, image.height)
    )
    return parsed_answer
app = Flask(__name__)

@app.route('/process-image', methods=['POST'])
def process_image():
    if 'file' not in request.files:
        return jsonify({'error': 'No file provided'}), 400

    file = request.files['file']
    try:
        image = Image.open(file.stream)
    except Exception as e:
        return jsonify({'error': 'Invalid image file'}), 400

    output = {}
    output['text'] = pytesseract.image_to_string(image, lang="eng+rus")

    caption_prompt = '<MORE_DETAILED_CAPTION>'
    caption_results = run(caption_prompt, image=image)
    output['caption'] = caption_results.get(caption_prompt)

    objects_prompt = '<CAPTION_TO_PHRASE_GROUNDING>'
    objects_results = run(objects_prompt, text_input=output['caption'], image=image)
    output['objects'] = objects_results.get(objects_prompt)

    tags_prompt = '<OD>'
    tags_results = run(tags_prompt, image=image)
    output['tags'] = tags_results.get(tags_prompt).get("labels")

    return jsonify(output)

if __name__ == '__main__':
    app.run()
