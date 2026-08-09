# Adding New Languages to CryptoSafe

## How to Add a New Language

### Step 1: Create translation files
Create a directory `{code}/` (e.g. `de/` for German) with these files:
- `main.json` — UI strings (160 keys)
- `help.json` — Help screen content (33 keys)
- `about.json` — About screen content (10 keys)

Copy the structure from the `en/` directory and translate all values.

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

That's it. The language menu, device detection, and persistence work automatically. New `.json` files added to the language directory are loaded automatically — no code changes needed.

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

## File Structure

Each language has its own directory with 3 files:

| File | Content | Keys |
|------|---------|------|
| `main.json` | UI, settings, boxes, security | 160 |
| `help.json` | Help & Guide screen content | 33 |
| `about.json` | About screen (version, how-to, disclaimer) | 10 |

The `LocalizationManager` automatically scans the directory and merges all `.json` files — adding a new file requires no code changes.

## Notes

- The language menu is **dynamic** — it reads from `getAvailableLocales()` automatically. No UI code changes needed.
- Device language is detected on first launch and falls back to English if unsupported.
- The user's language choice is saved persistently.

## Contributing

1. Fork the repository
2. Add your language file + the two code changes above
3. Create a Pull Request
