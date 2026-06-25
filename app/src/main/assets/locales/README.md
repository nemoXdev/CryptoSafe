# Adding New Languages to CryptoSafe

## How to Add a New Language

### Step 1: Create the translation file
Create `{code}.json` in this directory (e.g. `de.json` for German).
Copy the structure from `en.json` and translate all values.

### Step 2: Register the language in code
Open `LocalizationManager.kt` and:

**Add loading** (line ~22):
```kotlin
loadLocale(context, "de")
```

**Add display name** (line ~63):
```kotlin
"de" -> "Deutsch"
```

That's it. The language menu, device detection, and persistence work automatically.

## Language Codes

| Code | Language | Native Name |
|------|----------|-------------|
| `en` | English | English |
| `ar` | Arabic | العربية |
| `fr` | French | Français |
| `es` | Spanish | Español |
| `de` | German | Deutsch |
| `zh` | Chinese (Simplified) | 简体中文 |
| `pt` | Portuguese | Português |
| `fa` | Persian | فارسی |
| `ku` | Kurdish (Sorani) | کوردی |
| `hi` | Hindi | हिन्दी |

## File Format

Each language file must be valid JSON with these keys:

```json
{
  "app_name": "",
  "home": "",
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
  "weak": "",
  "medium": "",
  "strong": "",
  "copied": "",
  "error": "",
  "success": "",
  "language": "",
  "select_language": "",
  "offline_secure": "",
  "offline_desc": "",
  "input_label": "",
  "encrypt_text": "",
  "decrypt_text": "",
  "enter_password": "",
  "confirm_password": "",
  "output": "",
  "password_too_weak": "",
  "passwords_dont_match": "",
  "about": "",
  "about_title": "",
  "developer": "",
  "version": "",
  "description": "",
  "whats_new_title": "",
  "whats_new_body": "",
  "how_to_use_title": "",
  "how_to_use": "",
  "disclaimer_title": "",
  "disclaimer_body": ""
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
