---
name: pixel-grid-texture-gen
description: >-
  Guía y paleta de colores oficial para generar texturas pixel-art de bloques (256x256 en rejilla 16x16)
  e ítems con fondo 100% transparente (32x32) para sqlmod.
---

# Skill: Generación de Texturas Pixel-Grid y Modelos (`pixel-grid-texture-gen`)

Usa esta habilidad cuando crees o modifiques texturas de bloques, terminales o ítems en `assets/sqlmod/textures/`.

## 1. Estándar de Texturas de Bloques (`assets/sqlmod/textures/block/`)
- **Resolución:** `256x256` píxeles reales organizados en una **rejilla estricta de `16x16` celdas** (cada celda mide `16x16` píxeles con un bisel sutil de 1px para nitidez en cerca y lejos).
- **Sin desenfoque:** Nunca apliques interpolación bilineal/bicúbica que difumine los bordes entre celdas.
- **Paleta de Colores Oficial:**
  - Acero oscuro del chasis: `RGB(24, 26, 34)` / `RGB(38, 42, 54)` / `RGB(54, 60, 76)`
  - Esquinas reforzadas carmesí: `RGB(160, 32, 32)` / `RGB(205, 45, 45)`
  - Fondo de pantalla CRT: `RGB(8, 22, 16)` / `RGB(14, 40, 28)`
  - Texto y cursores SQL verde fósforo: `RGB(40, 224, 80)` / `RGB(120, 255, 150)`
  - Acentos inalámbricos / escáner: `RGB(0, 220, 230)` (cian) y `RGB(255, 180, 0)` (oro)
  - Acentos de infección bio-digital: `RGB(180, 40, 220)` / `RGB(240, 90, 255)`

## 2. Estándar de Texturas de Ítems (`assets/sqlmod/textures/item/`)
- **Resolución:** `32x32` píxeles.
- **Transparencia:** El fondo fuera de la silueta del ítem **DEBE** ser `RGBA(0, 0, 0, 0)` (100% transparente).
- **Contorno:** Incluye siempre un borde exterior oscuro (`RGB(18, 20, 28)`) alrededor de la silueta del ítem para que resalte en el inventario y en la mano del jugador.
