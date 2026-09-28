"""Переводит SVG-значки от дизайнеров в PNG и WebP для приложения.

Зачем: значки приходят из Figma в SVG, а в них внутренние тени — фильтры
(feOffset, feComposite). VectorDrawable фильтров не умеет, поэтому значок
растрируется. Рендерит headless Chrome: он понимает SVG ровно как Figma,
и отдельную библиотеку (cairosvg и т. п.) ставить не нужно.

Масштаб 4× — под xxxhdpi: значок в 56 dp на плотности 4.0 занимает 224 px,
и растр не растягивается ни на одном телефоне. Лежит в drawable-nodpi, потому что
размер задаёт код, а не плотность экрана.

Вход:  design/exports/ui/icons/*.svg
Выход: design/exports/ui/icons/*.png — растр для дизайнеров и ревью,
       app/src/main/res/drawable-nodpi/ic_<имя>.webp — без потерь.

Запуск из корня репозитория (нужны Chrome и Pillow):
    py tools/render_icons.py
"""

import re
import subprocess
import tempfile
from pathlib import Path

from PIL import Image

SRC = Path("design/exports/ui/icons")
RES = Path("app/src/main/res/drawable-nodpi")
SCALE = 4

CHROME_PATHS = [
    Path(r"C:\Program Files\Google\Chrome\Application\chrome.exe"),
    Path(r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"),
    Path("/usr/bin/google-chrome"),
    Path("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"),
]


def chrome() -> Path:
    for path in CHROME_PATHS:
        if path.exists():
            return path
    raise SystemExit("Не найден Chrome: он нужен, чтобы отрисовать SVG с фильтрами")


def render(svg: Path, out: Path) -> None:
    text = svg.read_text(encoding="utf-8")
    size = re.search(r'<svg[^>]*width="([\d.]+)"[^>]*height="([\d.]+)"', text)
    if size is None:
        raise SystemExit(f"{svg.name}: у <svg> нет width/height")
    width, height = (round(float(v)) for v in size.groups())

    with tempfile.TemporaryDirectory() as tmp:
        page = Path(tmp) / "icon.html"
        page.write_text(
            f'<html><body style="margin:0;background:transparent">{text}</body></html>',
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(chrome()),
                "--headless=new",
                "--disable-gpu",
                "--hide-scrollbars",
                # Прозрачный фон: без флага Chrome подкладывает белый.
                "--default-background-color=00000000",
                f"--force-device-scale-factor={SCALE}",
                f"--window-size={width},{height}",
                f"--screenshot={out.resolve()}",
                page.resolve().as_uri(),
            ],
            check=True,
            capture_output=True,
        )


def main() -> None:
    svgs = sorted(SRC.glob("*.svg"))
    if not svgs:
        raise SystemExit(f"В {SRC} нет SVG")
    for svg in svgs:
        png = svg.with_suffix(".png")
        render(svg, png)
        image = Image.open(png).convert("RGBA")
        webp = RES / f"ic_{svg.stem}.webp"
        image.save(webp, "WEBP", lossless=True, method=6)
        print(f"{svg.name} -> {png.name} {image.size[0]}x{image.size[1]}, {webp.name} {webp.stat().st_size // 1024} КБ")


if __name__ == "__main__":
    main()
