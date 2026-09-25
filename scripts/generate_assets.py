#!/usr/bin/env python3
"""
WATZEM Mobile App Asset Generator
Generates pixel-perfect, high-res native assets for Android, iOS, and Web:
- App Icons (iOS 1024x1024, Android mipmap hdpi/mdpi/xhdpi/xxhdpi/xxxhdpi, adaptive foreground, round)
- Native Splash Screens (iOS 2732x2732, Android port & land all densities)
- Web & PWA Icons (apple-touch-icon, icon-192, icon-512, favicon)
"""

import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.dirname(SCRIPT_DIR)

# Typography font finder
FONT_CANDIDATES = [
    '/System/Library/Fonts/Supplemental/Arial Bold.ttf',
    '/System/Library/Fonts/HelveticaNeue.ttc',
    '/System/Library/Fonts/SFNS.ttf',
    '/Library/Fonts/Arial Bold.ttf'
]

FONT_REGULAR_CANDIDATES = [
    '/System/Library/Fonts/Supplemental/Arial.ttf',
    '/System/Library/Fonts/HelveticaNeue.ttc',
    '/System/Library/Fonts/SFNS.ttf',
    '/Library/Fonts/Arial.ttf'
]

def get_font(size: int, bold: bool = True):
    candidates = FONT_CANDIDATES if bold else FONT_REGULAR_CANDIDATES
    for c in candidates:
        if os.path.exists(c):
            try:
                return ImageFont.truetype(c, size)
            except Exception:
                pass
    return ImageFont.load_default()

def draw_emblem(draw: ImageDraw.ImageDraw, x0: float, y0: float, size: float, corner_radius: float = None):
    """
    Renders the official WATZEM emblem into the specified bounding box (x0, y0, x0+size, y0+size)
    Based on WatzemLogo.vue:
    - Base plate with golden gradient
    - Glass sheen bevel
    - Left wing of 'W'
    - Right play chevron
    - Lens sparkle
    """
    if corner_radius is None:
        corner_radius = size * (12.0 / 48.0)
    
    # Rounded plate
    # Plate bounds: 3..45 in 48 grid -> margin = 3/48
    margin = size * (3.0 / 48.0)
    rect_box = [x0 + margin, y0 + margin, x0 + size - margin, y0 + size - margin]
    
    # Amber gradient plate
    # Generate vertical gradient for the squircle
    plate_w = int(rect_box[2] - rect_box[0])
    plate_h = int(rect_box[3] - rect_box[1])
    if plate_w > 0 and plate_h > 0:
        plate_img = Image.new("RGBA", (plate_w, plate_h), (0, 0, 0, 0))
        pdraw = ImageDraw.Draw(plate_img)
        for y in range(plate_h):
            t = y / max(1, plate_h - 1)
            # Top #fbbf24 (251, 191, 36) -> Mid #f59e0b (245, 158, 11) -> Bottom #d97706 (217, 119, 6)
            if t < 0.5:
                sub_t = t * 2.0
                r = int(251 + (245 - 251) * sub_t)
                g = int(191 + (158 - 191) * sub_t)
                b = int(36 + (11 - 36) * sub_t)
            else:
                sub_t = (t - 0.5) * 2.0
                r = int(245 + (217 - 245) * sub_t)
                g = int(158 + (119 - 158) * sub_t)
                b = int(11 + (6 - 11) * sub_t)
            pdraw.line([(0, y), (plate_w, y)], fill=(r, g, b, 255))
        
        # Mask for rounded rectangle
        mask = Image.new("L", (plate_w, plate_h), 0)
        m舖 = ImageDraw.Draw(mask)
        m舖.rounded_rectangle([0, 0, plate_w, plate_h], radius=int(corner_radius), fill=255)
        
        # Paste gradient plate
        draw._image.paste(plate_img, (int(rect_box[0]), int(rect_box[1])), mask)
    
    # Glass sheen border
    draw.rounded_rectangle(rect_box, radius=int(corner_radius), outline=(255, 255, 255, 80), width=max(1, int(size / 48.0)))
    
    # Coordinates map in 48x48 box
    def pt(px, py):
        return (x0 + size * (px / 48.0), y0 + size * (py / 48.0))
    
    # Left wing of 'W'
    # Coordinates: (12, 16) -> (16.5, 30) -> (20, 19) -> (23, 23) -> (21.5, 28)
    w_pts = [pt(12, 16), pt(16.5, 30), pt(20, 19), pt(23, 23), pt(21.5, 28)]
    stroke_w = max(2, int(size * (3.0 / 48.0)))
    
    for i in range(len(w_pts) - 1):
        draw.line([w_pts[i], w_pts[i+1]], fill=(255, 255, 255, 255), width=stroke_w)
    
    # Round joints and caps
    cap_r = stroke_w / 2.0
    for p in w_pts:
        draw.ellipse([p[0] - cap_r, p[1] - cap_r, p[0] + cap_r, p[1] + cap_r], fill=(255, 255, 255, 255))
    
    # Right Play Chevron
    # Coordinates: (25, 18) -> (35, 24) -> (25, 30)
    chev_pts = [pt(25, 18), pt(35, 24), pt(25, 30)]
    draw.polygon(chev_pts, fill=(255, 255, 255, 255))
    
    # Lens sparkle at (34, 14), r = 2.2
    sp_center = pt(34, 14)
    sp_r = size * (2.2 / 48.0)
    draw.ellipse([sp_center[0] - sp_r, sp_center[1] - sp_r, sp_center[0] + sp_r, sp_center[1] + sp_r], fill=(255, 255, 255, 255))
    
    # Sparkle glint rays
    flare_len = sp_r * 2.2
    flare_w = max(1, int(stroke_w * 0.45))
    draw.line([(sp_center[0] - flare_len, sp_center[1]), (sp_center[0] + flare_len, sp_center[1])], fill=(255, 255, 255, 220), width=flare_w)
    draw.line([(sp_center[0], sp_center[1] - flare_len), (sp_center[0], sp_center[1] + flare_len)], fill=(255, 255, 255, 220), width=flare_w)

def create_amber_gradient(width: int, height: int) -> Image.Image:
    """
    Generates a rich, full-bleed amber-to-gold linear gradient from top-left to bottom-right.
    Top-left: #fbbf24 (251, 191, 36) -> Mid: #f59e0b (245, 158, 11) -> Bottom-right: #b45309 (180, 83, 9)
    """
    gw, gh = 256, 256
    g_img = Image.new("RGB", (gw, gh))
    gdraw = ImageDraw.Draw(g_img)
    for y in range(gh):
        for x in range(gw):
            t = (x / gw * 0.45) + (y / gh * 0.55)
            if t < 0.5:
                st = t * 2.0
                r = int(251 + (245 - 251) * st)
                g = int(191 + (158 - 191) * st)
                b = int(36 + (11 - 36) * st)
            else:
                st = (t - 0.5) * 2.0
                r = int(245 + (180 - 245) * st)
                g = int(158 + (83 - 158) * st)
                b = int(11 + (9 - 11) * st)
            gdraw.point((x, y), fill=(r, g, b))
    return g_img.resize((width, height), Image.Resampling.BICUBIC).convert("RGBA")

def draw_watzem_symbol(draw: ImageDraw.ImageDraw, cx: float, cy: float, scale: float, color=(255, 255, 255, 255)):
    """
    Renders the official WATZEM white glyph mark (W wing, Play Chevron, Lens Sparkle).
    In 48x48 space:
    - W: (12, 16) -> (16.5, 30) -> (20, 19) -> (23, 23) -> (21.5, 28)
    - Chevron: (25, 18) -> (35, 24) -> (25, 30)
    - Sparkle: (34, 14), r=2.2
    Center point in 48 grid is (23.5, 22.0).
    """
    def pt(x48, y48):
        return (cx + (x48 - 23.5) * scale, cy + (y48 - 22.0) * scale)

    # Left wing of 'W'
    w_pts = [pt(12, 16), pt(16.5, 30), pt(20, 19), pt(23, 23), pt(21.5, 28)]
    stroke_w = max(2, int(3.0 * scale))

    for i in range(len(w_pts) - 1):
        draw.line([w_pts[i], w_pts[i+1]], fill=color, width=stroke_w)

    cap_r = stroke_w / 2.0
    for p in w_pts:
        draw.ellipse([p[0] - cap_r, p[1] - cap_r, p[0] + cap_r, p[1] + cap_r], fill=color)

    # Right Play Chevron
    chev_pts = [pt(25, 18), pt(35, 24), pt(25, 30)]
    draw.polygon(chev_pts, fill=color)

    # Radiant Lens Sparkle at (34, 14), r = 2.2
    sp_center = pt(34, 14)
    sp_r = 2.2 * scale
    draw.ellipse([sp_center[0] - sp_r, sp_center[1] - sp_r, sp_center[0] + sp_r, sp_center[1] + sp_r], fill=color)

    # Sparkle glint rays
    flare_len = sp_r * 2.0
    flare_w = max(1, int(stroke_w * 0.38))
    flare_color = (color[0], color[1], color[2], int(color[3] * 0.9)) if len(color) == 4 else color
    draw.line([(sp_center[0] - flare_len, sp_center[1]), (sp_center[0] + flare_len, sp_center[1])], fill=flare_color, width=flare_w)
    draw.line([(sp_center[0], sp_center[1] - flare_len), (sp_center[0], sp_center[1] + flare_len)], fill=flare_color, width=flare_w)

def create_master_icon(size: int = 1024) -> Image.Image:
    """
    Creates the master full-color 1024x1024 app icon.
    Edge-to-edge golden amber gradient with NO black corners,
    subtle specular glass sheen, soft warm shadow, and crisp white logo mark.
    """
    ss = size * 2
    img = create_amber_gradient(ss, ss)

    # Subtle top-left specular highlight sheen
    sheen = Image.new("RGBA", (ss, ss), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(sheen)
    sdraw.ellipse([int(ss * -0.25), int(ss * -0.25), int(ss * 0.85), int(ss * 0.75)], fill=(255, 255, 255, 42))
    sheen = sheen.filter(ImageFilter.GaussianBlur(ss * 0.14))
    img.alpha_composite(sheen)

    cx, cy = ss // 2, int(ss * 0.49)
    scale = (ss * 0.56) / 24.5

    # Soft warm drop shadow under the logo mark for depth
    shadow = Image.new("RGBA", (ss, ss), (0, 0, 0, 0))
    sh_draw = ImageDraw.Draw(shadow)
    sh_offset = ss * 0.016
    draw_watzem_symbol(sh_draw, cx, cy + sh_offset, scale, color=(140, 60, 0, 160))
    shadow = shadow.filter(ImageFilter.GaussianBlur(ss * 0.016))
    img.alpha_composite(shadow)

    # Crisp white WATZEM logo mark
    draw = ImageDraw.Draw(img)
    draw_watzem_symbol(draw, cx, cy, scale, color=(255, 255, 255, 255))

    return img.resize((size, size), Image.Resampling.LANCZOS)

def create_adaptive_foreground(size: int = 432) -> Image.Image:
    """
    Android adaptive icon foreground: transparent canvas with the white logo mark
    positioned comfortably inside the 66% safe circle (diameter ~264px in 432px).
    """
    ss = size * 2
    img = Image.new("RGBA", (ss, ss), (0, 0, 0, 0))

    cx, cy = ss // 2, int(ss * 0.49)
    # Safe scale ensures symbol width is ~44% of total canvas (well within 66% safe zone)
    scale = (ss * 0.44) / 24.5

    # Soft warm drop shadow
    shadow = Image.new("RGBA", (ss, ss), (0, 0, 0, 0))
    sh_draw = ImageDraw.Draw(shadow)
    sh_offset = ss * 0.014
    draw_watzem_symbol(sh_draw, cx, cy + sh_offset, scale, color=(140, 60, 0, 150))
    shadow = shadow.filter(ImageFilter.GaussianBlur(ss * 0.014))
    img.alpha_composite(shadow)

    # Crisp white WATZEM logo mark
    draw = ImageDraw.Draw(img)
    draw_watzem_symbol(draw, cx, cy, scale, color=(255, 255, 255, 255))

    return img.resize((size, size), Image.Resampling.LANCZOS)

def create_round_icon(size: int = 192) -> Image.Image:
    """
    Android circular round icon (ic_launcher_round.png):
    Full-color amber gradient circle with crisp white logo.
    Outside circle is transparent (NO black corners).
    """
    master = create_master_icon(size)
    mask = Image.new("L", (size, size), 0)
    mdraw = ImageDraw.Draw(mask)
    mdraw.ellipse([0, 0, size - 1, size - 1], fill=255)

    output = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    output.paste(master, (0, 0), mask)
    return output

def create_legacy_icon(size: int = 192) -> Image.Image:
    """
    Legacy Android squircle icon (ic_launcher.png):
    Full-color amber gradient squircle with crisp white logo.
    Outside squircle is transparent (NO black corners).
    """
    master = create_master_icon(size)
    mask = Image.new("L", (size, size), 0)
    mdraw = ImageDraw.Draw(mask)
    radius = int(size * 0.22)
    mdraw.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)

    output = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    output.paste(master, (0, 0), mask)
    return output

def create_splash_screen(width: int, height: int) -> Image.Image:
    """
    Renders high-definition splash screen with cinema background, amber ambient glow,
    centered WATZEM logo emblem, and bold cinematic typography.
    """
    # Supersample at 1.5x for performance and ultra crispness
    scale = 1.5 if max(width, height) <= 1920 else 1.0
    sw = int(width * scale)
    sh = int(height * scale)
    
    img = Image.new("RGBA", (sw, sh), (7, 7, 10, 255))
    
    # Central amber aura
    cx, cy = sw // 2, int(sh * 0.44)
    glow_r = int(min(sw, sh) * 0.55)
    
    glow = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    for r in range(glow_r, 0, -6):
        alpha = int(36 * (1.0 - (r / glow_r) ** 1.6))
        gdraw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(245, 158, 11, alpha))
    glow = glow.filter(ImageFilter.GaussianBlur(min(sw, sh) * 0.05))
    img.alpha_composite(glow)
    
    # Determine emblem size
    emblem_size = int(min(sw, sh) * 0.28)
    ex0 = cx - emblem_size // 2
    ey0 = cy - int(emblem_size * 0.72)
    
    # Shadow under emblem
    shadow = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow)
    sh_margin = emblem_size * (3.0 / 48.0)
    sh_rad = emblem_size * (12.0 / 48.0)
    sdraw.rounded_rectangle(
        [ex0 + sh_margin, ey0 + sh_margin + emblem_size * 0.06, ex0 + emblem_size - sh_margin, ey0 + emblem_size - sh_margin + emblem_size * 0.06],
        radius=int(sh_rad),
        fill=(0, 0, 0, 160)
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(emblem_size * 0.05))
    img.alpha_composite(shadow)
    
    # Draw emblem
    draw = ImageDraw.Draw(img)
    draw_emblem(draw, ex0, ey0, emblem_size)
    
    # Typography: "WATZ" in White, "EM" in Amber
    # Sizing
    font_title_size = int(emblem_size * 0.38)
    title_font = get_font(font_title_size, bold=True)
    
    title_y = ey0 + emblem_size + int(emblem_size * 0.22)
    
    # Measure text
    # "WATZ" and "EM"
    watz_bbox = title_font.getbbox("WATZ")
    em_bbox = title_font.getbbox("EM")
    watz_w = watz_bbox[2] - watz_bbox[0]
    em_w = em_bbox[2] - em_bbox[0]
    total_title_w = watz_w + em_w
    
    title_x0 = cx - total_title_w // 2
    
    # Draw "WATZ"
    draw.text((title_x0, title_y), "WATZ", fill=(255, 255, 255, 255), font=title_font)
    
    # Draw "EM" with amber color
    draw.text((title_x0 + watz_w, title_y), "EM", fill=(245, 158, 11, 255), font=title_font)
    
    # Subtitle: "CINEMA STREAM"
    sub_font_size = int(font_title_size * 0.26)
    sub_font = get_font(sub_font_size, bold=True)
    sub_text = "C I N E M A   S T R E A M"
    sub_bbox = sub_font.getbbox(sub_text)
    sub_w = sub_bbox[2] - sub_bbox[0]
    sub_y = title_y + font_title_size + int(font_title_size * 0.32)
    
    draw.text((cx - sub_w // 2, sub_y), sub_text, fill=(255, 255, 255, 140), font=sub_font)
    
    if scale != 1.0:
        return img.resize((width, height), Image.Resampling.LANCZOS)
    return img

def main():
    print("🎬 Generating WATZEM Mobile App Logos & Native Splash Screens...")
    
    # 1. Master Icon (iOS AppIcon 1024x1024)
    print("Generating iOS Master AppIcon (1024x1024)...")
    master_icon_1024 = create_master_icon(1024)
    ios_icon_path = os.path.join(PROJECT_DIR, "ios/App/App/Assets.xcassets/AppIcon.appiconset/AppIcon-512@2x.png")
    os.makedirs(os.path.dirname(ios_icon_path), exist_ok=True)
    master_icon_1024.convert("RGB").save(ios_icon_path, "PNG", optimize=True)
    print(f"  ✓ Saved {ios_icon_path}")
    
    # Web & PWA Icons
    public_dir = os.path.join(PROJECT_DIR, "public")
    os.makedirs(public_dir, exist_ok=True)
    
    master_icon_1024.resize((512, 512), Image.Resampling.LANCZOS).save(os.path.join(public_dir, "icon-512.png"), "PNG")
    master_icon_1024.resize((192, 192), Image.Resampling.LANCZOS).save(os.path.join(public_dir, "icon-192.png"), "PNG")
    master_icon_1024.resize((180, 180), Image.Resampling.LANCZOS).save(os.path.join(public_dir, "apple-touch-icon.png"), "PNG")
    master_icon_1024.resize((64, 64), Image.Resampling.LANCZOS).save(os.path.join(public_dir, "favicon.ico"), "ICO")
    print("  ✓ Saved Web / PWA icons in public/")
    
    # 2. Android Mipmap Icons
    android_res = os.path.join(PROJECT_DIR, "android/app/src/main/res")
    mipmap_configs = [
        ("mipmap-mdpi", 48, 108),
        ("mipmap-hdpi", 72, 162),
        ("mipmap-xhdpi", 96, 216),
        ("mipmap-xxhdpi", 144, 324),
        ("mipmap-xxxhdpi", 192, 432),
    ]
    
    print("Generating Android Mipmap Icons & Adaptive Foregrounds...")
    for folder, icon_sz, fg_sz in mipmap_configs:
        out_dir = os.path.join(android_res, folder)
        os.makedirs(out_dir, exist_ok=True)
        
        # Standard legacy squircle icon (transparent exterior, full color amber inside)
        std_icon = create_legacy_icon(icon_sz)
        std_icon.save(os.path.join(out_dir, "ic_launcher.png"), "PNG")
        
        # Round icon
        rnd_icon = create_round_icon(icon_sz)
        rnd_icon.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG")
        
        # Adaptive foreground
        fg_icon = create_adaptive_foreground(fg_sz)
        fg_icon.save(os.path.join(out_dir, "ic_launcher_foreground.png"), "PNG")
        print(f"  ✓ {folder}: {icon_sz}x{icon_sz}, fg {fg_sz}x{fg_sz}")
    
    # 3. iOS Splash Screens (2732x2732)
    print("Generating iOS Native Splash Screens (2732x2732)...")
    ios_splash = create_splash_screen(2732, 2732)
    ios_splash_dir = os.path.join(PROJECT_DIR, "ios/App/App/Assets.xcassets/Splash.imageset")
    for fname in ["splash-2732x2732.png", "splash-2732x2732-1.png", "splash-2732x2732-2.png"]:
        p = os.path.join(ios_splash_dir, fname)
        ios_splash.convert("RGB").save(p, "PNG", optimize=True)
        print(f"  ✓ Saved {p}")
    
    # 4. Android Native Splash Screens
    print("Generating Android Native Splash Screens (Portrait & Landscape)...")
    android_splash_configs = [
        # Default fallback
        ("drawable", 480, 320),
        # Portrait
        ("drawable-port-mdpi", 320, 480),
        ("drawable-port-hdpi", 480, 800),
        ("drawable-port-xhdpi", 720, 1280),
        ("drawable-port-xxhdpi", 960, 1600),
        ("drawable-port-xxxhdpi", 1280, 1920),
        # Landscape
        ("drawable-land-mdpi", 480, 320),
        ("drawable-land-hdpi", 800, 480),
        ("drawable-land-xhdpi", 1280, 720),
        ("drawable-land-xxhdpi", 1600, 960),
        ("drawable-land-xxxhdpi", 1920, 1280),
    ]
    
    for folder, w, h in android_splash_configs:
        dpath = os.path.join(android_res, folder)
        os.makedirs(dpath, exist_ok=True)
        out_file = os.path.join(dpath, "splash.png")
        s_img = create_splash_screen(w, h)
        s_img.convert("RGB").save(out_file, "PNG", optimize=True)
        print(f"  ✓ {folder}/splash.png: {w}x{h}")
    
    print("\n✨ All WATZEM mobile app splash screens and logos built successfully!")

if __name__ == "__main__":
    main()
