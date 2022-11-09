from keybert import KeyBERT

kw_model = KeyBERT()

def extract_keywords(text):
    keywords = kw_model.extract_keywords(text)
    return keywords