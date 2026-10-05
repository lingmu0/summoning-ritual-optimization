# Summoning Ritual Optimization

JEI display support for mob outputs from Summoning Rituals and Goety rituals.

The repository uses separate branches for each game version:

- `mc-1.20.1` — Forge 1.20.1; supports Summoning Rituals and Goety.
- `mc-1.21.1` — NeoForge 1.21.1; supports Summoning Rituals and Goety 3.2.0.

JEI, Summoning Rituals, and Goety are optional dependencies. This mod only changes
the JEI recipe display. Mob outputs use their spawn egg when available, otherwise a
structure void; the slot tooltip shows the mob name and output count.

Build with `gradlew build` on the matching branch.
