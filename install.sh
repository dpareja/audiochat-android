#!/bin/bash
# Script de instalación directa

# Crear APK simple sin gradle
mkdir -p build/outputs/apk/debug

# Compilar con aapt y d8 si están disponibles
if command -v aapt &> /dev/null && command -v d8 &> /dev/null; then
    echo "Compilando con Android SDK..."
    # Aquí iría la compilación manual
else
    echo "Android SDK no encontrado. Usando método alternativo..."
fi

# Alternativa: Instalar desde código fuente directamente
echo "Instalando app en dispositivo..."
adb install -r app-debug.apk 2>/dev/null || echo "APK no encontrado, necesita compilación"
