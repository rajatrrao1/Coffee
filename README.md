# 珈 Coffee

A coffee bean log for the Mudita Kompakt, in the style of the tea app: each bag you
drink, where it is from, its altitude, and how you brew it (water temperature, grind,
dose, ratio, time).

- **Add from a link.** Press **+ Add**, paste the coffee's page and press **Fetch**,
  or share the page from the browser and pick **Add to Coffee**. The app reads the
  name, roaster, origin, region, altitude, process, variety, producer, roast and
  tasting notes it can find, fills only empty fields, and tells you what the page
  did not say. Check the fields before saving.
- **Your brew** sits at the top of each coffee's screen, for reading at the kettle.
- **Filter** by roast, or show finished bags. **Search** looks through everything.
- **Backup** saves all beans to one JSON file, brings them back in, or sends the
  list as text.

Everything stays on the phone. The only network use is fetching a page you asked for.

## Building

GitHub builds it: every change pushed to this repository runs `.github/workflows/build.yml`,
which publishes `coffee.apk` as the latest release:

    https://github.com/<you>/<repo>/releases/latest/download/coffee.apk

On a computer with the Android SDK and Gradle 8.9: `gradle assembleRelease`.

The signing key in `signing/` is there so every build installs over the previous one
without losing your beans. It is only for this personal app.
