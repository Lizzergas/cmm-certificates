# Sertifikatų generatorius

Programa skirta pažymėjimų generavimui iš XLSX registracijos duomenų ir DOCX šablono, PDF failų paruošimui bei jų siuntimui el. paštu.

## Kaip naudotis
1. Pasirinkite XLSX failą su registracijos duomenimis.
2. Pasirinkite DOCX šabloną.
3. Užpildykite sertifikato laukus (ID, valandos, pavadinimas, lektorius ir kt.).
4. Spauskite „Peržiūrėti PDF“ arba „Konvertuoti į PDF“.
5. Jei trūksta failų ar laukų, ekranas pažymės konkrečias klaidas ties XLSX / DOCX pasirinkimu ir pačiais formos laukais.
6. Jei DOCX šablone nėra tam tikros žymės, atitinkamas laukas bus išjungtas ir užvedus pelę bus parodyta priežastis.
7. Desktop/JVM programoje failai pagal nutylėjimą bus sugeneruoti į `~/pdf/<sertifikato_pavadinimas>`, bet išsaugojimo aplanką galima pasirinkti per programos aplanko parinkiklį.

## Pastabos
- PDF generavimas vyksta lokaliai JVM programoje ir konvertavimui interneto ryšys nebėra privalomas.
- El. laiškų siuntimui vis tiek reikia sukonfigūruoto SMTP ir veikiančio ryšio.
- Jei laiškų nepavyksta išsiųsti, jie išsaugomi pakartotiniam siuntimui vėliau.
- Pakartotinis siuntimas tikrina, ar sugeneruoti PDF failai vis dar egzistuoja diske.
- Dienos el. laiškų limitas `0` reiškia neribotą siuntimą.

## XLSX duomenų schema
Programa šiuo metu remiasi fiksuota pirmo lapo stulpelių tvarka:

1. data / registracijos laikas
2. pirminis el. paštas
3. vardas
4. pavardė
5. įstaiga
6. renginys
7. mokėjimo nuoroda
8. viešinimo sutikimas

Tuščia pirmo stulpelio reikšmė sustabdo tolimesnį eilučių skaitymą.

## Šablono žymės
DOCX šablone naudokite žymes:
- `{{vardas_pavarde}}`
- `{{data}}`
- `{{akreditacijos_id}}`
- `{{dokumento_id}}`
- `{{akreditacijos_tipas}}`
- `{{akreditacijos_valandos}}`
- `{{sertifikato_pavadinimas}}`
- `{{destytojas}}`
- `{{destytojo_tipas}}`

Pastabos apie žymes:
- Konvertavimo ekranas patikrina, kurios žymės realiai yra pasirinktame DOCX faile.
- Jei žymės nėra, su ja susietas formos laukas taps neaktyvus ir nebus laikomas privalomu.
- `{{dokumento_id}}` laukas lieka aktyvus net tada, kai šios žymės nėra, nes jis vis tiek naudojamas PDF failų vardams.

## Nauja eilutė šablone
Jei norite įterpti naują eilutę lauke:
- naudokite `\n`, arba
- naudokite `w:br`

Abi reikšmės bus paverstos į Word eilutės lūžį (nauja eilutė).

## Platformų palaikymas
- JVM / Desktop: pilnas srautas - XLSX, DOCX -> PDF, peržiūros laiškas, masinis siuntimas, nepavykusių siuntimų talpykla.
- Android / iOS: rodoma, kurios funkcijos dar nepalaikomos; XLSX skaitymas, DOCX -> PDF generavimas, SMTP siuntimas ir sugeneruotų aplankų atidarymas ten dar neišbaigti.

## Projekto struktūra
- `androidApp/` - Android programos entry point, manifestas ir Android resursai.
- `composeApp/` - plonas KMP app shell: `App.kt`, `Navigator.kt`, DI agregavimas, desktop entry point ir iOS `MainViewController`.
- `core/` - bendri resursai, tema, UI primityvai, `expect/actual`, logging, i18n, platform capability abstractions.
- `feature/settings/` - SMTP, el. laisko sablonai, signature editor, nustatymu saugojimas.
- `feature/certificate/` - XLSX nuskaitymas, DOCX sablono uzpildymas, PDF generavimo workflow ir conversion ekranas.
- `feature/pdfconversion/` - konvertavimo progreso ir preview email ekranas.
- `feature/emailsending/` - masinis siuntimas, retry ir siuntimo progresas.

## Build / test komandos
- `./gradlew :composeApp:run` - paleidzia desktop/JVM programa.
- `./gradlew :composeApp:jvmTest` - JVM testai.
- `./gradlew :composeApp:test` - Android host testai KMP app shell moduliui.
- `./gradlew :androidApp:assembleDebug` - Android debug APK.

## Naudingi dokumentai
- `FEATURES.md` - produkto srautas ir techninės pastabos
- `EMAIL.md` - SMTP ir siuntimo elgsena
- `PDF_GEN.md` - DOCX -> PDF pipeline JVM platformoje
- `docs/architecture.md` - dabartinė repo architektūra

## Windows release
Windows `.msi` diegiklis surenkamas per GitHub Actions workflow `.github/workflows/windows-msi-on-tag.yml`.

Trumpa procedūra:
1. Įsitikinkite, kad pakeitimai yra commit'inti ir išpushinti į GitHub.
2. Sukurkite tag'ą formatu `vMAJOR.MINOR.BUILD`, pvz. `v1.0.1`.
3. Išpushinkite tag'ą:
   `git push origin v1.0.1`
4. GitHub automatiškai paleis Windows build'ą ir sugeneruos `.msi` artefaktą.
5. Atsidarykite repo `Actions` skiltį ir atsisiųskite artefaktą iš paskutinio `Build Windows MSI` paleidimo.

Pavyzdys:
`./scripts/bump-release-tag.sh`

# D.U.K.
- Jeigu nepavyksta tam tikriems laiškams būti išsiųstiems, jie bus išsaugomi ir bus galima pamėginti pakartotina išsiųsti
