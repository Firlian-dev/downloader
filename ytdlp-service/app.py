#!/usr/bin/env python3
"""
yt-dlp HTTP service wrapper
This service provides an HTTP API for executing yt-dlp commands from a remote application.
"""

import os
import json
import subprocess
import tempfile
from flask import Flask, request, jsonify, send_file
from pathlib import Path

app = Flask(__name__)

# Configuration
DOWNLOAD_DIR = os.environ.get('DOWNLOAD_DIR', '/downloads')
Path(DOWNLOAD_DIR).mkdir(parents=True, exist_ok=True)


@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({'status': 'ok', 'service': 'yt-dlp-http-service'})


@app.route('/version', methods=['GET'])
def version():
    """Get yt-dlp version"""
    try:
        result = subprocess.run(
            ['yt-dlp', '--version'],
            capture_output=True,
            text=True,
            check=True
        )
        return jsonify({
            'version': result.stdout.strip(),
            'service': 'yt-dlp-http-service'
        })
    except subprocess.CalledProcessError as e:
        return jsonify({'error': str(e)}), 500


@app.route('/metadata', methods=['POST'])
def get_metadata():
    """
    Get metadata for a URL without downloading
    Request body: { "url": "https://..." }
    """
    data = request.get_json()
    if not data or 'url' not in data:
        return jsonify({'error': 'URL is required'}), 400
    
    url = data['url']
    
    try:
        # Add --cookies-from-browser to try to use browser cookies for authenticated content
        result = subprocess.run(
            ['yt-dlp', '--dump-json', '--no-warnings', '--no-check-certificates', url],
            capture_output=True,
            text=True,
            check=True,
            timeout=30
        )
        
        # Parse JSON output
        metadata = json.loads(result.stdout)
        return jsonify(metadata)
        
    except subprocess.TimeoutExpired:
        print(f"Timeout getting metadata for {url}")
        return jsonify({'error': 'Request timeout'}), 408
    except subprocess.CalledProcessError as e:
        error_msg = e.stderr if e.stderr else str(e)
        print(f"yt-dlp error for {url}: {error_msg}")
        return jsonify({'error': f'yt-dlp error: {error_msg}'}), 500
    except json.JSONDecodeError as e:
        print(f"JSON decode error for {url}: {e}")
        return jsonify({'error': 'Invalid JSON response from yt-dlp'}), 500


@app.route('/download', methods=['POST'])
def download():
    """
    Download media from URL
    Request body: { 
        "url": "https://...",
        "itemIndex": 0 (optional)
    }
    Returns: { "filePath": "/downloads/...", "fileName": "...", "sizeBytes": 123 }
    """
    data = request.get_json()
    if not data or 'url' not in data:
        return jsonify({'error': 'URL is required'}), 400
    
    url = data['url']
    item_index = data.get('itemIndex', 0)
    
    try:
        # Build output template
        output_template = os.path.join(DOWNLOAD_DIR, '%(title)s-%(id)s.%(ext)s')
        
        # Build command
        cmd = [
            'yt-dlp',
            '--no-warnings',
            '--no-playlist',
            '-o', output_template,
            url
        ]
        
        # Add playlist item selection if needed
        if item_index > 0:
            cmd.extend(['--playlist-items', str(item_index + 1)])
        
        # Execute download
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            check=True,
            timeout=300  # 5 minutes timeout
        )
        
        # Find the downloaded file
        # Parse output to find the file path
        output_lines = result.stdout.split('\n')
        downloaded_file = None
        
        for line in output_lines:
            if 'Destination:' in line or 'has already been downloaded' in line:
                parts = line.split(':', 1)
                if len(parts) > 1:
                    downloaded_file = parts[1].strip()
                    break
        
        # If not found in output, find the most recent file
        if not downloaded_file:
            files = list(Path(DOWNLOAD_DIR).glob('*'))
            files = [f for f in files if f.is_file() and not f.name.startswith('.')]
            if files:
                downloaded_file = str(max(files, key=lambda f: f.stat().st_mtime))
        
        if not downloaded_file:
            return jsonify({'error': 'Could not determine downloaded file path'}), 500
        
        # Get file info
        file_path = Path(downloaded_file)
        if not file_path.exists():
            return jsonify({'error': f'Downloaded file not found: {downloaded_file}'}), 500
        
        return jsonify({
            'filePath': str(file_path),
            'fileName': file_path.name,
            'sizeBytes': file_path.stat().st_size
        })
        
    except subprocess.TimeoutExpired:
        print(f"Timeout downloading {url}")
        return jsonify({'error': 'Download timeout (max 5 minutes)'}), 408
    except subprocess.CalledProcessError as e:
        error_msg = e.stderr if e.stderr else str(e)
        print(f"Download failed for {url}: {error_msg}")
        return jsonify({'error': f'Download failed: {error_msg}'}), 500


if __name__ == '__main__':
    # Run on all interfaces, port 8090
    app.run(host='0.0.0.0', port=8090, debug=False)
