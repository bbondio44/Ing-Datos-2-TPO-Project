# Guía rápida: Claude Code para el TP

## 1. Copiar los archivos al repo

Copiá todo el contenido de este kit a la raíz del repositorio (donde está `pom.xml` o `build.gradle`):

| Archivo | Para qué sirve | ¿Se sube a git? |
|---|---|---|
| `CLAUDE.md` | Contexto del proyecto que Claude Code lee en cada sesión | Sí, lo comparten los tres |
| `CLAUDE.local.md` | Tus preferencias personales (explicaciones, tu módulo) | No |
| `.claude/settings.json` | Qué comandos puede correr sin preguntar y qué tiene prohibido | Sí |
| `docs/modelo-fisico-mongodb-cassandra.md` | Tu modelo físico; `CLAUDE.md` lo importa | Sí |
| `docker-compose.yml` | Las 4 bases en local. Si el repo ya tiene uno, quedate con ese | Sí |
| `.env.example` | Plantilla de credenciales | Sí |

Después agregá esto al `.gitignore`:

```
.env
CLAUDE.local.md
.claude/settings.local.json
```

Y creá tu `.env`:

```bash
cp .env.example .env    # y cambiá las contraseñas
docker compose up -d
docker compose ps       # esperá a que Cassandra figure healthy
```

## 2. Primera sesión

Abrí una terminal en la raíz del repo y ejecutá `claude`. La primera vez te pregunta si confiás en la
carpeta: decí que sí (sin eso no aplica los permisos de `.claude/settings.json`).

Primer mensaje sugerido:

> Revisá el repo y completá la sección "Comandos" del CLAUDE.md con los comandos reales de build,
> tests y para correr la app. Decime qué build tool y versión de Java usamos.

Segundo mensaje sugerido, para entender dónde estás parado:

> Mirá qué hay implementado de MongoDB y Cassandra y compará con docs/modelo-fisico-mongodb-cassandra.md.
> Armame una lista de qué falta para mis requisitos (R09, R17–R26, R32, R40). No cambies nada todavía.

## 3. Cómo trabajar requisito por requisito

1. Entrá en modo plan (`Shift+Tab` hasta que diga Plan) y pedí: "Planificá R17". Leé el plan,
   corregilo si hace falta y aprobalo.
2. Dejá que implemente. Revisá cada diff antes de aceptarlo.
3. Pedile que corra los tests y que te deje la consulta o request que demuestra el requisito.
4. Pedile que te explique el código como si te lo fuera a preguntar el profesor.
5. Commit: "commiteá con mensaje R17: ..." (va a hacer el commit; el push te lo pregunta).
6. `/clear` antes de pasar al próximo requisito, así arranca con el contexto limpio.

## 4. Comandos útiles dentro de Claude Code

| Comando | Qué hace |
|---|---|
| `Shift+Tab` | Cambia de modo: manual, aceptar ediciones automáticamente, plan |
| `Esc` | Interrumpe lo que está haciendo para corregir el rumbo |
| `Esc Esc` | Vuelve a un punto anterior de la conversación (y del código) |
| `/clear` | Empieza una conversación nueva en la misma carpeta |
| `/compact` | Resume la conversación cuando se hace muy larga |
| `/permissions` | Ver y editar qué puede hacer sin preguntar |
| `/memory` | Editar `CLAUDE.md` y `CLAUDE.local.md` |
| `claude -c` | (desde la terminal) Retoma la última conversación |

## 5. Para la defensa

El profesor va a pedir modificar algo en vivo. Antes de la entrega:

- Hacé vos, sin Claude Code, al menos un cambio chico en Mongo y otro en Cassandra (agregar un campo,
  una consulta nueva) para confirmar que sabés dónde vive cada cosa.
- Pedile: "Haceme 10 preguntas que me podría hacer el profesor sobre mi modelo de Cassandra y
  MongoDB, sin darme las respuestas". Respondelas y después pedile que te corrija.
