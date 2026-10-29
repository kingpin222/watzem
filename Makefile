# WATZEM Makefile
.PHONY: all app mac it install startup status stop clean help

all: app

# Support "make it" or "make mac" or "make app"
it: app
mac: app

app:
	@echo "🍿 Building WATZEM macOS Application..."
	node scripts/build-mac-app.js --install --startup

install:
	@echo "📦 Installing WATZEM to /Applications and setting up startup..."
	node scripts/build-mac-app.js --install --startup

startup:
	@echo "🔄 Enabling WATZEM macOS Startup (Login Item + Background Service)..."
	node scripts/mac-startup.js --enable-all

status:
	@echo "📊 Checking WATZEM macOS Service Status..."
	node scripts/mac-startup.js --status

service:
	@echo "⚙️ Installing launchd background service..."
	node scripts/mac-startup.js --enable-service

stop:
	@echo "🛑 Disabling WATZEM startup and stopping service..."
	node scripts/mac-startup.js --disable-all

clean:
	@echo "🧹 Cleaning built .app bundle..."
	rm -rf WATZEM.app
