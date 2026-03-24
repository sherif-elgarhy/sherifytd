#!/usr/bin/env bash
# SherifYTD — Installer

REPO_DIR="$(cd "$(dirname "$0")" && pwd)"
echo "$REPO_DIR"
sleep 1

echo ""
echo "📥 SherifYTD Installer"
echo "──────────────────────────"

# 1. Storage access (Termux only)
if [[ -n "$TERMUX_VERSION" || "$HOME" == *com.termux* ]]; then
  if [[ ! -d "$HOME/storage/shared" ]]; then
    echo "📂 Setting up storage access..."
    termux-setup-storage
    echo "⏳ Waiting for storage..."
    while [[ ! -d "$HOME/storage/shared" ]]; do sleep 1; done
  fi
fi

# 2. Set up ~/bin
if [[ ! -d "$HOME/bin" ]]; then
  echo "📁 Creating ~/bin..."
  mkdir -p "$HOME/bin"
fi

# 3. Copy termux-url-opener
cp "$REPO_DIR/termux-url-opener" "$HOME/bin/termux-url-opener"
chmod +x "$HOME/bin/termux-url-opener"
echo "✅ termux-url-opener installed to ~/bin/"

# 4. Make ytd.sh executable
chmod +x "$REPO_DIR/ytd.sh"
echo "✅ ytd.sh is ready"

# 5. Run ytd.sh once to trigger dependency install
echo ""
echo "🔧 Running first-time setup (dependencies)..."
bash "$REPO_DIR/ytd.sh"

echo ""
echo "🎉 SherifYTD is ready!"
echo "📲 Share any video link to Termux to download"
echo "💡 Or run: ~/sherifytd/ytd.sh"

