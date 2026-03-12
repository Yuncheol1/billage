import os
import re

FILES_TO_EDIT = [
    r"app\src\main\java\com\billage\app\ui\chat\components\ChatRoomComponents.kt",
    r"app\src\main\java\com\billage\app\ui\chat\components\ContractDocumentFullScreen.kt",
    r"app\src\main\java\com\billage\app\ui\chat\components\cards\BaseChatCard.kt",
    r"app\src\main\java\com\billage\app\ui\chat\components\cards\ContractCards.kt",
    r"app\src\main\java\com\billage\app\ui\chat\components\cards\MediaCards.kt"
]

REPLACEMENTS = {
    r"MaterialTheme\.colorScheme\.onPrimaryContainer": "BillageTextPrimary",
    r"MaterialTheme\.colorScheme\.primaryContainer": "BillagePrimaryLight",
    r"MaterialTheme\.colorScheme\.onPrimary": "Color.White",
    r"MaterialTheme\.colorScheme\.primary": "BillagePrimary",
    r"MaterialTheme\.colorScheme\.onSurfaceVariant": "BillageTextSecondary",
    r"MaterialTheme\.colorScheme\.surfaceVariant": "BillageDivider",
    r"MaterialTheme\.colorScheme\.onSurface": "BillageTextPrimary",
    r"MaterialTheme\.colorScheme\.surface": "BillageSurface",
    r"MaterialTheme\.colorScheme\.onBackground": "BillageTextPrimary",
    r"MaterialTheme\.colorScheme\.background": "BillageBackground",
    r"MaterialTheme\.colorScheme\.secondary": "BillageInfo",
    r"MaterialTheme\.colorScheme\.error": "BillageDanger",
    r"MaterialTheme\.colorScheme\.outlineVariant": "BillageDivider",
    r"MaterialTheme\.colorScheme\.outline": "BillageTextSecondary"
}

for filepath in FILES_TO_EDIT:
    full_path = os.path.join(os.getcwd(), filepath)
    if not os.path.exists(full_path):
        print(f"File not found: {full_path}")
        continue
    
    with open(full_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Apply regex replacements ensuring we don't accidentally match substrings
    new_content = content
    for target, replacement in REPLACEMENTS.items():
        # \b ensures word boundary, but colorScheme ends with a word. 
        # so target doesn't need \b at start.
        new_content = re.sub(target + r"(?!\w)", replacement, new_content)
        
    # Add import if missing
    import_stmt = "import com.billage.app.ui.theme.*\n"
    if "import com.billage.app.ui.theme" not in new_content:
        new_content = new_content.replace(
            "import androidx.compose.ui.Modifier",
            import_stmt + "import androidx.compose.ui.Modifier",
            1
        )
        
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    
    print(f"Processed: {filepath}")
