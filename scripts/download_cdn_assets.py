#!/usr/bin/env python3
"""
CDN Asset Downloader for Engla / AILearn KMP
Downloads all Firebase Storage CDN assets into local directory cdn_assets_backup/
for easy re-uploading to new cloud storage.
"""

import os
import json
import urllib.request
import urllib.parse
import sys

BUCKET = "apl567-tutor-ai-language.firebasestorage.app"
BASE_URL = f"https://firebasestorage.googleapis.com/v0/b/{BUCKET}/o/"
OUTPUT_DIR = "/Volumes/data/AndroidStudioProjects/ailearn/cdn_assets_backup"

def download_file(rel_path, is_required=True):
    encoded_path = urllib.parse.quote(rel_path, safe='')
    url = f"{BASE_URL}{encoded_path}?alt=media"
    local_path = os.path.join(OUTPUT_DIR, rel_path)
    
    if os.path.exists(local_path) and os.path.getsize(local_path) > 0:
        print(f"[EXISTS] {rel_path} ({os.path.getsize(local_path)} bytes)")
        return local_path
        
    os.makedirs(os.path.dirname(local_path), exist_ok=True)
    
    print(f"[DOWNLOADING] {rel_path} <- {url}")
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=30) as resp:
            content = resp.read()
            with open(local_path, 'wb') as f:
                f.write(content)
        print(f"[SUCCESS] {rel_path} ({len(content)} bytes)")
        return local_path
    except Exception as e:
        if is_required:
            print(f"[ERROR] Failed to download {rel_path}: {e}", file=sys.stderr)
        else:
            print(f"[OPTIONAL_SKIP] {rel_path}: {e}")
        return None

def main():
    print(f"Starting CDN Assets Download to {OUTPUT_DIR}...")
    
    # 1. Download Master Catalogs
    cat_path = download_file("shadowing/catalog.json")
    plan_path = download_file("plan/plan.json")
    manifest_path = download_file("plan/content/manifest.json")
    
    # 2. Parse Shadowing Catalog
    if cat_path and os.path.exists(cat_path):
        with open(cat_path, 'r', encoding='utf-8') as f:
            cat_data = json.load(f)
            videos = cat_data.get('videos', [])
            print(f"\nProcessing {len(videos)} video shadowing items...")
            for v in videos:
                vid_id = v.get('id', 'unknown')
                script = v.get('script') # e.g. scripts/ordering_breakfast.json
                thumb = v.get('thumb')   # e.g. thumbs/ordering_breakfast.jpg
                clip = v.get('clip')     # e.g. clips/ordering_breakfast.mp4
                
                if script:
                    download_file(f"shadowing/{script}")
                if thumb:
                    download_file(f"shadowing/{thumb}")
                if clip:
                    # Video clips are larger mp4 files
                    download_file(f"shadowing/{clip}", is_required=False)

    # 3. Parse Plan Manifest
    if manifest_path and os.path.exists(manifest_path):
        with open(manifest_path, 'r', encoding='utf-8') as f:
            try:
                manifest_data = json.load(f)
                files = manifest_data.get('files', []) if isinstance(manifest_data, dict) else manifest_data
                print(f"\nProcessing {len(files)} plan manifest items...")
                for item in files:
                    if isinstance(item, str):
                        download_file(f"plan/content/{item}", is_required=False)
                    elif isinstance(item, dict) and 'path' in item:
                        download_file(f"plan/content/{item['path']}", is_required=False)
            except Exception as e:
                print(f"Error parsing manifest: {e}")

    print("\n[ALL DONE] CDN Assets backup completed!")

if __name__ == '__main__':
    main()
