# Adding New Languages to CryptoSafe

## How to Add a New Language

### Step 1: Create the translation file
Create `{code}.json` in this directory (e.g. `de.json` for German).
Copy the structure from `en.json` and translate all values.

### Step 2: Register the language in code
Open `LocalizationManager.kt` and:

**Add loading**:
```kotlin
loadLocale(context, "de")
```

**Add display name**:
```kotlin
"de" -> "Deutsch"
```

That's it. The language menu, device detection, and persistence work automatically.

## Language Codes

| Code | Language | Native Name |
|------|----------|-------------|
| `en` | English | English |
| `ar` | Arabic | العربية |
| `de` | German | Deutsch |
| `es` | Spanish | Español |
| `fa` | Persian | فارسی |
| `fr` | French | Français |
| `hi` | Hindi | हिन्दी |
| `id` | Indonesian | Bahasa Indonesia |
| `ja` | Japanese | 日本語 |
| `ko` | Korean | 한국어 |
| `ku` | Kurdish (Sorani) | کوردی |
| `pt` | Portuguese | Português |
| `ru` | Russian | Русский |
| `tr` | Turkish | Türkçe |
| `zh` | Chinese (Simplified) | 简体中文 |

## File Format

Each language file must be valid JSON with these keys:

```json
{
  "app_name": "",
  "encrypt": "",
  "decrypt": "",
  "password": "",
  "input_text": "",
  "output_text": "",
  "copy": "",
  "clear": "",
  "encrypt_button": "",
  "decrypt_button": "",
  "password_strength": "",
  "count": "",
  "weak": "",
  "medium": "",
  "strong": "",
  "copied": "",
  "error": "",
  "success": "",
  "select_language": "",
  "offline_secure": "",
  "offline_desc": "",
  "input_label": "",
  "output": "",
  "password_required": "",
  "password_too_weak": "",
  "decrypt_error": "",
  "about": "",
  "developer": "",
  "source_code": "",
  "version": "",
  "whats_new_title": "",
  "whats_new_body": "",
  "how_to_use_title": "",
  "how_to_use": "",
  "disclaimer_title": "",
  "disclaimer_body": "",
  "content_desc_back": "",
  "content_desc_language": "",
  "content_desc_about": "",
  "generate_password": "",
  "length": ""
}
```

## Notes

- The language menu is **dynamic** — it reads from `getAvailableLocales()` automatically. No UI code changes needed.
- Device language is detected on first launch and falls back to English if unsupported.
- The user's language choice is saved persistently.

## Contributing

1. Fork the repository
2. Add your language file + the two code changes above
3. Create a Pull Request
