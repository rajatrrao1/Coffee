# 珈 Coffee

A coffee bean log for the Mudita Kompakt: each bag you
drink, where it is from, its altitude, and how you brew it (water temperature, grind,
dose, ratio, time).

- **Add from a link.** Press **+ Add**, paste the coffee's page and press **Fetch**,
  or share the page from the browser and pick **Add to Coffee**. The app reads the
  name, roaster, origin, region, altitude, process, variety, producer, roast and
  tasting notes it can find, fills only empty fields, and tells you what the page
  did not say. Check the fields before saving.
- **Your brew** sits at the top of each coffee's screen, for reading at the kettle.
  Choose the method (espresso, pour-over, AeroPress, French press, moka pot, cold brew)
  and the form shows what that method needs: for espresso the coffee in, the yield in
  the cup and the shot time; for pour-over the bloom and the pours; for cold brew the
  steep time in hours, and so on. Coffee, ratio and yield stay in step: 17 g at 1:2
  fills in 34 g.
- **The bag**: its size and price, the day you opened it and the day it ran out.
  Each coffee shows how long the bag lasted and about how many grams a day you drank.
  **Add another bag of this coffee** starts a fresh bag with the same details.
- **Filter** by roast, or show finished bags. **Search** looks through everything.
- **More** shows how much coffee you bought this year and in all, what it cost, how
  long a bag lasts on average and how much you drink a day.
- **Backup folder**: choose a folder once (under More) and the app keeps
  `coffee-beans.json` there, updated after every change, so your beans survive even
  removing the app. It can also bring beans in from that file, or send the list as text.

Everything stays on the phone. The only network use is fetching a page you asked for.

<img width="922" height="2048" alt="image" src="https://github.com/user-attachments/assets/d92cbfca-186b-49eb-bed0-e135a2de03d7" />
<img width="480" height="800" alt="image" src="https://github.com/user-attachments/assets/a789ead9-eaa0-4147-9d0d-ed4801d5bfa9" />

<img width="922" height="2048" alt="image" src="https://github.com/user-attachments/assets/6c34cce7-56c4-46bd-b6ae-f5a03d9be511" />


## Building

GitHub builds it: every change pushed to this repository runs `.github/workflows/build.yml`,
which publishes `coffee.apk` as the latest release:

    https://github.com/<you>/<repo>/releases/latest/download/coffee.apk

On a computer with the Android SDK and Gradle 8.9: `gradle assembleRelease`.

The signing key in `signing/` is there so every build installs over the previous one
without losing your beans. It is only for this personal app.
