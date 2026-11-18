---
name: build-and-deploy
description: >-
  Procedimiento para compilar sqlmod (vía Gradle o javac directo con mappings SRG de Forge 1.20.1-47.4.20),
  empaquetar el JAR y exportarlo a la instancia de CurseForge.
---

# Skill: Compilación y Despliegue (`build-and-deploy`)

Usa esta habilidad para compilar cambios de código o recursos y actualizar el archivo `.jar` del mod tanto en `build/libs/` como en la instancia de pruebas de CurseForge.

## 1. Compilación Rápida con `javac` sobre Mappings SRG (Funciona incluso sin OneDrive activo)
Cuando necesites compilar clases específicas o cuando `src/` tenga archivos bajo demanda de OneDrive:
```powershell
$srgJar = "C:\Users\rojoh\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.20\forge-1.20.1-47.4.20-srg.jar"
$binPatched = "C:\Users\rojoh\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.20\forge-1.20.1-47.4.20-binpatched.jar"
$univJar = "C:\Users\rojoh\.gradle\caches\forge_gradle\maven_downloader\net\minecraftforge\forge\1.20.1-47.4.20\forge-1.20.1-47.4.20-universal.jar"
$extraJars = Get-ChildItem -Recurse "C:\Users\rojoh\.gradle\caches\modules-2\files-2.1" -Filter "*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -match "eventbus|fmlcore|javafmllanguage|mergetool|blaze3d|brigadier|datafixerupper|guava|gson|joml|slf4j|log4j|commons-lang3|fastutil|netty" } | Select-Object -ExpandProperty FullName
$cp = (@($extractedDir, $binPatched, $srgJar, $univJar) + $extraJars) -join ";"
& javac -encoding UTF-8 --release 17 -cp $cp -d $extractedDir $javaFiles
```

## 2. Empaquetado y Exportación a CurseForge
```powershell
$jarExe = (Get-Command javac).Source -replace "javac\.exe$","jar.exe"
$outJar = "C:\Users\rojoh\curseforge\minecraft\Instances\DungeonsPrub\mods\sqlmod-1.0.0.jar"
& $jarExe cfm $outJar "$extractedDir\META-INF\MANIFEST.MF" -C $extractedDir .
Copy-Item $outJar -Destination "C:\Users\rojoh\OneDrive\Desktop\sqlMod\build\libs\sqlmod-1.0.0.jar" -Force
```

## 3. Verificación Post-Build
Ejecuta siempre `& $jarExe tf $outJar` para verificar que:
- `META-INF/mods.toml` y `pack.mcmeta` estén presentes.
- `logo.png` esté en la raíz del `.jar`.
- No existan archivos temporales `.java` o scripts dentro del `.jar`.
