import os
import re
import shutil

base_dir = r"c:\S14P21A305\be\src\main\java\com\ssafy\a305\be"

# Map exactly which class goes to which package
class_to_pkg = {
    # global/exception
    "CustomBusinessException": "com.ssafy.a305.be.global.exception",
    "GlobalExceptionHandler": "com.ssafy.a305.be.global.exception",
    "ErrorCode": "com.ssafy.a305.be.global.exception.dto",
    "ErrorResponse": "com.ssafy.a305.be.global.exception.dto",

    # domain/user
    "User": "com.ssafy.a305.be.domain.user.entity",
    "UserProfile": "com.ssafy.a305.be.domain.user.entity",
    "UserConsent": "com.ssafy.a305.be.domain.user.entity",
    "SocialAuth": "com.ssafy.a305.be.domain.user.entity",
    "UserRepository": "com.ssafy.a305.be.domain.user.repository",
    "TermsMaster": "com.ssafy.a305.be.domain.user.entity",

    # domain/contract
    "Contract": "com.ssafy.a305.be.domain.contract.entity",
    "ContractClause": "com.ssafy.a305.be.domain.contract.entity",
    "ClauseNegotiation": "com.ssafy.a305.be.domain.contract.entity",
    "ContractController": "com.ssafy.a305.be.domain.contract.controller",
    "ContractService": "com.ssafy.a305.be.domain.contract.service",
    "ContractRepository": "com.ssafy.a305.be.domain.contract.repository",
    "ContractDraftRequestDto": "com.ssafy.a305.be.domain.contract.dto.request",
    "ContractDraftResponseDto": "com.ssafy.a305.be.domain.contract.dto.response",

    # domain/repayment
    "RepaymentSchedule": "com.ssafy.a305.be.domain.repayment.entity",
    "RepaymentEvent": "com.ssafy.a305.be.domain.repayment.entity",
    "AutoTransferRegistration": "com.ssafy.a305.be.domain.repayment.entity",
    "EarlySettlement": "com.ssafy.a305.be.domain.repayment.entity",

    # domain/account
    "BankAccount": "com.ssafy.a305.be.domain.account.entity",
    "BankTransaction": "com.ssafy.a305.be.domain.account.entity",

    # domain/document
    "Document": "com.ssafy.a305.be.domain.document.entity",
    "Signature": "com.ssafy.a305.be.domain.document.entity",

    # domain/notification
    "NotificationLog": "com.ssafy.a305.be.domain.notification.entity",
    "FcmToken": "com.ssafy.a305.be.domain.notification.entity",
    "PromiseReminder": "com.ssafy.a305.be.domain.notification.entity",

    # domain/tax
    "TaxCalculation": "com.ssafy.a305.be.domain.tax.entity",
}

# 1. Gather all java files
java_files = []
for root, dirs, files in os.walk(base_dir):
    for f in files:
        if f.endswith(".java"):
            java_files.append(os.path.join(root, f))

# 2. Determine moves
moves = []
for path in java_files:
    fname = os.path.basename(path)
    cname = fname[:-5]
    if cname in class_to_pkg:
        new_pkg = class_to_pkg[cname]
        # convert package string to path relative to base_dir
        rel_pkg = new_pkg.replace("com.ssafy.a305.be", "").strip(".")
        if rel_pkg:
            rel_path = rel_pkg.replace(".", os.sep)
            new_dir = os.path.join(base_dir, rel_path)
            new_path = os.path.join(new_dir, fname)
            if path != new_path:
                moves.append((path, new_path, cname, new_pkg))

# 3. Create target dirs and rewrite package statements
for old_path, new_path, cname, new_pkg in moves:
    os.makedirs(os.path.dirname(new_path), exist_ok=True)
    with open(old_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # replace package declaration
    content = re.sub(r'package\s+[\w\.]+;', f'package {new_pkg};', content)
    
    with open(new_path, 'w', encoding='utf-8') as f:
        f.write(content)
        
    os.remove(old_path)

# 4. Gather java files again after moves
java_files = []
for root, dirs, files in os.walk(base_dir):
    for f in files:
        if f.endswith(".java"):
            java_files.append(os.path.join(root, f))

# 5. Fix imports globally
for path in java_files:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
        
    orig_content = content
    for cname, new_pkg in class_to_pkg.items():
        new_fqn = f"{new_pkg}.{cname}"
        # change import statements that end with cname;
        content = re.sub(r'import\s+[\w\.]+\.' + cname + r';', f'import {new_fqn};', content)
        
    if content != orig_content:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)

# 6. Delete empty folders
empty_dirs = True
while empty_dirs:
    empty_dirs = False
    for root, dirs, files in os.walk(base_dir, topdown=False):
        for d in dirs:
            dir_path = os.path.join(root, d)
            if not os.listdir(dir_path):
                os.rmdir(dir_path)
                empty_dirs = True

print("Refactoring completed successfully.")
