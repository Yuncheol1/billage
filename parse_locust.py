import re, json

def parse_html(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Try to find the const templateArgs = {...}
    match = re.search(r'const templateArgs\s*=\s*(\{.*?\})\s*</script>', content, re.DOTALL)
    if not match:
        return None
        
    try:
        data = json.loads(match.group(1).replace("'", '"'))  # extremely naive but may work if valid JSON
        # Wait, if templateArgs contains variable references, json.loads will fail.
        # Let's extract regular expressions instead for requests
    except Exception as e:
        pass
        
    requests_match = re.search(r'"requests"\s*:\s*(\[.*?\]),', content, re.DOTALL)
    if requests_match:
        try:
            reqs = json.loads(requests_match.group(1))
            agg = next((r for r in reqs if r.get('name') == 'Aggregated'), None)
            if agg: return agg
        except: pass
    
    return None

def extract_manual(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # regex extract aggregated stats
    # Aggregated stats are usually in the last object of the "requests" list
    # Let's just find the string `"name": "Aggregated"` and the surrounding object
    match = re.search(r'\{[^{]*"name":\s*"Aggregated"[^}]*\}', content)
    if match:
        try:
            return json.loads(match.group(0))
        except:
            pass
    return None

b = extract_manual('before_report_3users.html')
a = extract_manual('after_report_real.html')

def print_stats(name, d):
    if d:
        print(f"[{name}]")
        print(f"Reqs: {d.get('num_requests')}, Fails: {d.get('num_failures')}")
        print(f"Min: {d.get('min_response_time')} ms")
        print(f"Max: {d.get('max_response_time')} ms")
        print(f"Avg: {d.get('avg_response_time')} ms")
        print("-" * 20)

print_stats("BEFORE", b)
print_stats("AFTER", a)
