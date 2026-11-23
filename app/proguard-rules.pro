# R8 en release. Room, Hilt, Navigation y kotlinx.serialization traen sus propias reglas
# (consumer rules), así que aquí casi no hace falta nada.

# Mensajes de error legibles en reportes de crash.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# osmdroid no usa reflexión sobre nuestras clases; solo silenciamos avisos de clases opcionales.
-dontwarn org.osmdroid.**
