import os
from PIL import Image, ImageDraw, ImageFont

OUTPUT_DIR = r"d:\NIOM\Codex project\Android App\wake-me-there\play-store-assets"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# Helper to draw rounded rectangle with gradient-like look
def create_app_icon():
    size = (512, 512)
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Background gradient: Indigo / Deep Blue (0x1A237E) to Vibrant Cyan (0x0288D1)
    for y in range(512):
        ratio = y / 512.0
        r = int(26 + (3 - 26) * ratio)
        g = int(35 + (136 - 35) * ratio)
        b = int(126 + (209 - 126) * ratio)
        draw.line([(0, y), (512, y)], fill=(r, g, b, 255))

    # Rounded mask for app icon
    mask = Image.new("L", size, 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle([(0, 0), (512, 512)], radius=110, fill=255)
    img.putalpha(mask)

    draw = ImageDraw.Draw(img)

    # Concentric radar rings (white with low opacity)
    center = (256, 230)
    for r in [170, 125, 80]:
        draw.ellipse([center[0]-r, center[1]-r, center[0]+r, center[1]+r], outline=(255, 255, 255, 45), width=4)

    # Stylized Map Pin / Alarm Bell in center
    # Outer pin circle
    pin_r = 55
    draw.ellipse([center[0]-pin_r, center[1]-pin_r, center[0]+pin_r, center[1]+pin_r], fill=(255, 255, 255, 255))
    # Inner dot
    dot_r = 22
    draw.ellipse([center[0]-dot_r, center[1]-dot_r, center[0]+dot_r, center[1]+dot_r], fill=(2, 136, 209, 255))

    # Pin point pointing down
    draw.polygon([
        (center[0] - 38, center[1] + 30),
        (center[0] + 38, center[1] + 30),
        (center[0], center[1] + 115)
    ], fill=(255, 255, 255, 255))

    # Sound wave arc at top right
    draw.arc([center[0]-75, center[1]-110, center[0]+110, center[1]+75], start=280, end=350, fill=(255, 215, 0, 255), width=10)
    draw.arc([center[0]-95, center[1]-130, center[0]+130, center[1]+95], start=280, end=350, fill=(255, 215, 0, 200), width=8)

    # Alarm sound bell accent at bottom
    draw.text((160, 410), "WAKE ME", fill=(255, 255, 255, 230))
    draw.text((290, 410), "THERE", fill=(255, 215, 0, 255))

    path = os.path.join(OUTPUT_DIR, "app-icon-512.png")
    img.save(path, "PNG")
    print("Created:", path)

def create_feature_graphic():
    # 1024 x 500 px
    size = (1024, 500)
    img = Image.new("RGB", size, (15, 23, 42)) # Deep slate navy
    draw = ImageDraw.Draw(img)

    # Gradient overlay
    for x in range(1024):
        ratio = x / 1024.0
        r = int(15 + (30 - 15) * ratio)
        g = int(23 + (58 - 23) * ratio)
        b = int(42 + (138 - 42) * ratio)
        draw.line([(x, 0), (x, 500)], fill=(r, g, b))

    # Subtle radar rings on left
    radar_center = (240, 250)
    for r in [220, 160, 100, 40]:
        draw.ellipse([radar_center[0]-r, radar_center[1]-r, radar_center[0]+r, radar_center[1]+r], outline=(34, 211, 238, 40), width=2)

    # Pin point on radar
    draw.ellipse([radar_center[0]-25, radar_center[1]-25, radar_center[0]+25, radar_center[1]+25], fill=(34, 211, 238, 255))
    draw.ellipse([radar_center[0]-10, radar_center[1]-10, radar_center[0]+10, radar_center[1]+10], fill=(255, 255, 255, 255))

    # Glowing pulse
    draw.ellipse([radar_center[0]-45, radar_center[1]-45, radar_center[0]+45, radar_center[1]+45], outline=(34, 211, 238, 120), width=3)

    # Right Content Area: Brand Title, Taglines & Badges
    # App Name
    draw.text((450, 95), "WAKE ME THERE", fill=(255, 255, 255))
    draw.text((450, 130), "Smart GPS Transit & Location Alarm", fill=(34, 211, 238))

    # Main Pitch
    draw.text((450, 190), "Sleep Peacefully on Bus, Train & Commute.", fill=(241, 245, 249))
    draw.text((450, 220), "We Wake You Up Right Before Your Stop.", fill=(148, 163, 184))

    # 3 Benefit Pills
    pills = [
        ("100% PRIVATE", "Zero Data Leaves Your Phone"),
        ("SMART BATTERY GPS", "0% Drain When Inactive"),
        ("NATURAL VOICE ALERTS", "Spoken Arrival Reminders")
    ]
    
    start_y = 280
    for title, subtitle in pills:
        draw.rounded_rectangle([(450, start_y), (970, start_y + 52)], radius=10, fill=(30, 41, 59), outline=(51, 65, 85), width=1)
        draw.ellipse([(468, start_y + 20), (480, start_y + 32)], fill=(34, 211, 238))
        draw.text((495, start_y + 10), title, fill=(34, 211, 238))
        draw.text((495, start_y + 28), subtitle, fill=(203, 213, 225))
        start_y += 64

    path = os.path.join(OUTPUT_DIR, "feature-graphic-1024x500.png")
    img.save(path, "PNG")
    print("Created:", path)

def create_screenshot(index, headline, subheadline, card_content, accent_color=(34, 211, 238)):
    # 1080 x 1920 px (standard high-res portrait)
    size = (1080, 1920)
    img = Image.new("RGB", size, (11, 15, 25)) # Midnight deep background
    draw = ImageDraw.Draw(img)

    # Gradient top glow
    for y in range(450):
        ratio = (450 - y) / 450.0
        r = int(11 + accent_color[0] * 0.15 * ratio)
        g = int(15 + accent_color[1] * 0.15 * ratio)
        b = int(25 + accent_color[2] * 0.15 * ratio)
        draw.line([(0, y), (1080, y)], fill=(r, g, b))

    # Top Promotional Banner (Clean, High Trust Marketing)
    draw.text((80, 120), f"FEATURE 0{index}", fill=accent_color)
    draw.text((80, 180), headline, fill=(255, 255, 255))
    draw.text((80, 260), subheadline, fill=(148, 163, 184))

    # Phone Frame Mockup
    phone_box = [110, 360, 970, 1860]
    # Phone outer bezel
    draw.rounded_rectangle(phone_box, radius=56, fill=(26, 32, 44), outline=(74, 85, 104), width=4)
    # Phone screen inner
    screen_box = [130, 380, 950, 1840]
    draw.rounded_rectangle(screen_box, radius=42, fill=(15, 23, 42))

    # Top Status Bar in Phone
    draw.text((170, 410), "9:41", fill=(203, 213, 225))
    draw.text((840, 410), "5G  100%", fill=(203, 213, 225))

    # App Bar Header in Phone
    draw.text((170, 480), "Wake Me There", fill=(255, 255, 255))
    draw.text((170, 520), "Adaptive GPS • Battery Optimized", fill=accent_color)

    # Render Card Contents (specific to each feature)
    card_content(draw, screen_box, accent_color)

    path = os.path.join(OUTPUT_DIR, f"screenshot_{index}.png")
    img.save(path, "PNG")
    print("Created:", path)

# Screenshots definition
def card_screen_1(draw, box, accent):
    # Home Screen with 1-Tap Arming and Active Alarms
    # Quick commute section
    draw.text((170, 590), "Quick Commute • 1-Tap Arming", fill=accent)
    
    # Preset Cards
    draw.rounded_rectangle([(170, 630), (510, 780)], radius=24, fill=(30, 58, 138), outline=accent, width=2)
    draw.text((200, 655), "METRO", fill=accent)
    draw.text((420, 655), "ARMED", fill=(74, 222, 128))
    draw.text((200, 700), "Central Station", fill=(255, 255, 255))
    draw.text((200, 735), "Radius: 500 m • 2.4 km away", fill=(203, 213, 225))

    draw.rounded_rectangle([(530, 630), (870, 780)], radius=24, fill=(30, 41, 59))
    draw.text((560, 655), "BUS", fill=(148, 163, 184))
    draw.text((750, 655), "TAP TO ARM", fill=(148, 163, 184))
    draw.text((560, 700), "Downtown Stop", fill=(255, 255, 255))
    draw.text((560, 735), "Radius: 300 m", fill=(148, 163, 184))

    # Alarm Item Card
    draw.text((170, 830), "Saved Alarms", fill=(241, 245, 249))
    card_y = 880
    draw.rounded_rectangle([(170, card_y), (910, card_y + 360)], radius=32, fill=(30, 41, 59), outline=(51, 65, 85), width=2)
    draw.text((210, card_y + 30), "Office / Tech Park", fill=(255, 255, 255))
    draw.text((210, card_y + 70), "12.9352, 77.6245 • GPS Monitored", fill=(148, 163, 184))
    # Switch ON
    draw.rounded_rectangle([(800, card_y + 35), (870, card_y + 75)], radius=20, fill=accent)
    draw.ellipse([(835, card_y + 38), (867, card_y + 72)], fill=(255, 255, 255))

    # Badges
    pills = ["1.8 km away", "500 m radius", "Voice Alert", "1-Shot"]
    bx = 210
    for p in pills:
        draw.rounded_rectangle([(bx, card_y + 130), (bx + 150, card_y + 175)], radius=12, fill=(15, 23, 42))
        draw.text((bx + 20, card_y + 143), p, fill=accent)
        bx += 165

    # Status footer
    draw.line([(210, card_y + 210), (870, card_y + 210)], fill=(51, 65, 85), width=1)
    draw.text((210, card_y + 250), "Tracking GPS: 25s Balanced Power (Saving Battery)", fill=(74, 222, 128))
    draw.text((210, card_y + 290), "Arrival Trigger: ~3 minutes away", fill=(241, 245, 249))

def card_screen_2(draw, box, accent):
    # Map & Radius screen
    draw.text((170, 590), "Search Destination & Customize Radius", fill=accent)

    # Simulated Map Graphic Area
    map_box = [170, 640, 910, 1180]
    draw.rounded_rectangle(map_box, radius=28, fill=(17, 24, 39), outline=(55, 65, 81), width=2)
    # Radar grid
    mc = (540, 910)
    for r in [220, 150, 80]:
        draw.ellipse([mc[0]-r, mc[1]-r, mc[0]+r, mc[1]+r], outline=(34, 211, 238, 80), width=3)
    # Center Pin
    draw.ellipse([mc[0]-16, mc[1]-16, mc[0]+16, mc[1]+16], fill=(239, 68, 68))
    draw.ellipse([mc[0]-6, mc[1]-6, mc[0]+6, mc[1]+6], fill=(255, 255, 255))
    draw.text((mc[0]+25, mc[1]-15), "Grand Central Terminal", fill=(255, 255, 255))

    # Bottom config sheet
    sheet_y = 1220
    draw.rounded_rectangle([(170, sheet_y), (910, sheet_y + 550)], radius=36, fill=(30, 41, 59), outline=(51, 65, 85), width=2)
    draw.text((210, sheet_y + 35), "Grand Central Terminal", fill=(255, 255, 255))
    draw.text((210, sheet_y + 80), "Distance: 4.2 km away", fill=accent)
    draw.text((210, sheet_y + 140), "Trigger Radius Slider", fill=(203, 213, 225))

    # Slider bar
    draw.rounded_rectangle([(210, sheet_y + 190), (870, sheet_y + 204)], radius=8, fill=(51, 65, 85))
    draw.rounded_rectangle([(210, sheet_y + 190), (540, sheet_y + 204)], radius=8, fill=accent)
    draw.ellipse([(525, sheet_y + 180), (555, sheet_y + 214)], fill=(255, 255, 255))
    draw.text((750, sheet_y + 140), "500 m", fill=accent)

    # Confirm Button
    draw.rounded_rectangle([(210, sheet_y + 250), (870, sheet_y + 340)], radius=20, fill=accent)
    draw.text((360, sheet_y + 280), "SET ALARM FOR THIS AREA", fill=(11, 15, 25))

def card_screen_3(draw, box, accent):
    # Natural Voice Spoken Announcements
    draw.text((170, 590), "Natural Spoken Arrival Announcements", fill=accent)

    vy = 650
    draw.rounded_rectangle([(170, vy), (910, vy + 460)], radius=32, fill=(30, 41, 59), outline=accent, width=2)
    draw.text((210, vy + 35), "Spoken Voice Announcement", fill=(255, 255, 255))
    draw.text((210, vy + 75), "Speaks your destination out loud using offline neural TTS", fill=(148, 163, 184))

    # Voice Styles
    draw.text((210, vy + 140), "Announcement Styles:", fill=(241, 245, 249))
    styles = [
        ("Transit Commute (Recommended)", "\"Attention! Arriving at Mountain View. Please prepare to get off.\""),
        ("Wake-Up Alert", "\"Wake up! You have reached Mountain View! You have arrived!\""),
        ("Calm Reminder", "\"You have reached your destination: Mountain View.\"")
    ]
    sy = vy + 190
    for s_title, s_text in styles:
        draw.rounded_rectangle([(210, sy), (870, sy + 75)], radius=16, fill=(15, 23, 42), outline=(51, 65, 85), width=1)
        draw.text((230, sy + 12), s_title, fill=accent)
        draw.text((230, sy + 40), s_text, fill=(203, 213, 225))
        sy += 90

    # Test Button
    draw.rounded_rectangle([(170, vy + 490), (910, vy + 580)], radius=20, fill=(30, 58, 138), outline=accent, width=2)
    draw.text((330, vy + 520), "PLAY TEST VOICE ANNOUNCEMENT", fill=accent)

def card_screen_4(draw, box, accent):
    # Active Alarm Screen (Lockscreen wake-up)
    draw.text((170, 590), "Guaranteed Fullscreen Wake-Up", fill=(239, 68, 68))

    ay = 650
    draw.rounded_rectangle([(170, ay), (910, ay + 800)], radius=40, fill=(30, 27, 75), outline=(239, 68, 68), width=3)
    
    # Bell icon
    draw.ellipse([(500, ay + 60), (580, ay + 140)], fill=(239, 68, 68))
    draw.text((460, ay + 170), "ARRIVED!", fill=(239, 68, 68))
    draw.text((320, ay + 240), "Mountain View Station", fill=(255, 255, 255))
    draw.text((350, ay + 310), "You have entered your 500m trigger zone", fill=(203, 213, 225))

    # Pulsing radar
    draw.ellipse([(440, ay + 380), (640, ay + 580)], outline=(239, 68, 68, 150), width=4)
    draw.ellipse([(480, ay + 420), (600, ay + 540)], fill=(239, 68, 68))

    # Slide to dismiss bar
    draw.rounded_rectangle([(220, ay + 650), (860, ay + 740)], radius=45, fill=(15, 23, 42), outline=(239, 68, 68), width=2)
    draw.ellipse([(230, ay + 658), (300, ay + 728)], fill=(239, 68, 68))
    draw.text((360, ay + 680), "SLIDE TO TURN OFF ALARM >>", fill=(255, 255, 255))

def card_screen_5(draw, box, accent):
    # Privacy & Battery Smart
    draw.text((170, 590), "100% Private • Smart Battery Protection", fill=accent)

    py = 650
    draw.rounded_rectangle([(170, py), (910, py + 800)], radius=36, fill=(30, 41, 59), outline=(51, 65, 85), width=2)
    
    badges = [
        ("ZERO CLOUD TRACKING", "Your GPS coordinates are calculated entirely on your device and are never sent to external servers or sold to brokers."),
        ("ZERO BATTERY DRAIN WHEN IDLE", "When alarms are off, GPS hardware is completely shut down. No background battery waste."),
        ("ADAPTIVE DISTANCE TIERS", "Polls at 60s low-power mode when far away (>10 km), saving up to 85% battery, then seamlessly scales up on arrival."),
        ("WORKS 100% OFFLINE", "Alarms trigger reliably even in subway tunnels, underground metros, flights, and remote areas with zero cell signal.")
    ]

    by = py + 40
    for b_title, b_desc in badges:
        draw.rounded_rectangle([(210, by), (870, by + 160)], radius=20, fill=(15, 23, 42), outline=accent, width=1)
        draw.text((240, by + 18), b_title, fill=accent)
        draw.text((240, by + 55), b_desc, fill=(203, 213, 225))
        by += 180

def main():
    print("Generating Google Play Store Graphics...")
    create_app_icon()
    create_feature_graphic()
    create_screenshot(1, "Never Miss Your Stop Again", "1-Tap commute arming for bus, train, metro & road trips.", card_screen_1, accent_color=(56, 189, 248))
    create_screenshot(2, "Pinpoint Arrival Geofencing", "Search any address or drop a pin with a custom radius slider.", card_screen_2, accent_color=(34, 211, 238))
    create_screenshot(3, "Natural Voice Arrival Alerts", "Spoken announcements alert you by voice before you arrive.", card_screen_3, accent_color=(129, 140, 248))
    create_screenshot(4, "Reliable Fullscreen Wake-Up", "Loud alarms, smooth audio ducking & lockscreen override.", card_screen_4, accent_color=(248, 113, 113))
    create_screenshot(5, "100% Private & Battery Smart", "Zero telemetry. Adaptive GPS uses 85% less battery.", card_screen_5, accent_color=(74, 222, 128))
    print("All assets generated successfully!")

if __name__ == "__main__":
    main()
