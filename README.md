# AudioChat Android 📱

Aplicación Android de chat en tiempo real usando audio ultrasónico. Compatible con la versión de escritorio de AudioChat.

## Características

- 💬 **Chat en tiempo real** con PC y otros Android
- 🔇 **Ultrasónico**: Frecuencias 17-20.4 kHz (casi silencioso)
- 📡 **Broadcast**: Todos escuchan todos los mensajes
- 🔄 **Compatible**: Funciona con versión Python de escritorio
- 📱 **Nativo**: Kotlin + Android AudioRecord/AudioTrack
- 🎯 **Simple**: Interfaz minimalista

## Requisitos

- Android 7.0 (API 24) o superior
- Micrófono y altavoz que soporten >17 kHz
- Permisos de audio

## Instalación

### Opción 1: Android Studio

1. Abre el proyecto en Android Studio
2. Conecta tu dispositivo Android o inicia un emulador
3. Click en "Run" (▶️)

### Opción 2: Compilar APK

```bash
./gradlew assembleDebug
```

El APK estará en: `app/build/outputs/apk/debug/app-debug.apk`

## Uso

### En Android:

1. Abre la app AudioChat
2. Escribe tu nombre (máx 16 caracteres)
3. Escribe un mensaje (máx 64 caracteres)
4. Presiona "Enviar"

### En PC (Python):

```bash
python3 audio_chat.py TuNombre
```

### Chat entre Android y PC:

**Android:**
```
Nombre: Alice
Mensaje: Hola desde Android!
[Enviar]

[18:30:15] Bob: Hola Alice!
```

**PC (Python):**
```bash
$ python3 audio_chat.py Bob
[18:30:10] Alice: Hola desde Android!
> Hola Alice!
```

## Estructura del Proyecto

```
audiochat-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/audiochat/
│   │   │   └── MainActivity.kt      # Lógica principal
│   │   ├── res/layout/
│   │   │   └── activity_main.xml    # Interfaz UI
│   │   └── AndroidManifest.xml      # Configuración
│   └── build.gradle                 # Dependencias
├── build.gradle                     # Configuración proyecto
└── README.md
```

## Cómo Funciona

1. **AudioRecord**: Captura audio del micrófono a 44.1 kHz
2. **Detección**: Algoritmo Goertzel detecta frecuencias ultrasónicas
3. **Decodificación**: Convierte símbolos a texto
4. **AudioTrack**: Reproduce tonos ultrasónicos para enviar
5. **Broadcast**: Todos los dispositivos escuchan simultáneamente

## Protocolo

Compatible con AudioChat Python:
- 8-FSK (8 frecuencias, 3 bits/símbolo)
- Frecuencias: 17000, 17485, 17970, 18455, 18940, 19425, 19910, 20395 Hz
- Preámbulo: [0, 7, 0, 7]
- Formato: [username_len][username][message]

## Permisos

La app requiere:
- `RECORD_AUDIO`: Para escuchar mensajes
- `MODIFY_AUDIO_SETTINGS`: Para ajustar volumen

## Limitaciones

⚠ **Hardware**: No todos los dispositivos soportan >17 kHz
⚠ **Volumen**: Debe estar alto para buena recepción
⚠ **Distancia**: Limitado por alcance de altavoz/micrófono
⚠ **Colisiones**: Solo una persona puede hablar a la vez
⚠ **Batería**: Uso continuo de audio consume batería

## Troubleshooting

**No escucho mensajes:**
- Verifica permisos de micrófono
- Aumenta el volumen
- Acerca los dispositivos
- Verifica que tu hardware soporte >17 kHz

**Mensajes cortados:**
- Reduce distancia entre dispositivos
- Elimina ruido de fondo
- Aumenta volumen

**App crashea:**
- Verifica permisos en Configuración > Apps > AudioChat
- Reinicia la app
- Verifica Android 7.0+

## Compatibilidad

✅ **Compatible con:**
- AudioChat Python (escritorio)
- Otros dispositivos Android con la app
- Cualquier dispositivo que implemente el protocolo

❌ **No compatible con:**
- Dispositivos sin soporte ultrasónico
- Versiones Android <7.0

## Mejoras Futuras

- [ ] Detección automática de hardware
- [ ] Ajuste automático de volumen
- [ ] Historial de mensajes
- [ ] Notificaciones
- [ ] Modo oscuro
- [ ] Emojis
- [ ] Cifrado
- [ ] Salas privadas

## Desarrollo

### Requisitos de desarrollo:
- Android Studio Arctic Fox o superior
- JDK 11+
- Android SDK 33
- Kotlin 1.8+

### Compilar:
```bash
./gradlew build
```

### Ejecutar tests:
```bash
./gradlew test
```

## Licencia

MIT

## Créditos

Basado en:
- AudioChat: https://github.com/dpareja/audiochat
- AudioProtocol: https://github.com/dpareja/audioprotocol

## Contribuir

Pull requests bienvenidos! Para cambios mayores, abre un issue primero.
