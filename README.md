# Sertifikatų generatorius

Programa skirta pažymėjimų generavimui iš XLSX registracijos duomenų ir DOCX šablono, PDF failų paruošimui bei jų siuntimui el. paštu.

## Kaip naudotis
1. Pasirinkite XLSX failą su registracijos duomenimis.
2. Pasirinkite DOCX šabloną.
3. Užpildykite sertifikato laukus (ID, valandos, pavadinimas, lektorius ir kt.).
4. Spauskite „Konvertuoti į PDF“.
5. Failai bus sugeneruoti į `./pdf/<sertifikato_pavadinimas>`, o netinkami failo pavadinimo simboliai bus automatiškai pakeisti saugiais simboliais.

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

## Nauja eilutė šablone
Jei norite įterpti naują eilutę lauke:
- naudokite `\n`, arba
- naudokite `w:br`

Abi reikšmės bus paverstos į Word eilutės lūžį (nauja eilutė).

## Platformų palaikymas
- JVM / Desktop: pilnas srautas - XLSX, DOCX -> PDF, peržiūros laiškas, masinis siuntimas, nepavykusių siuntimų talpykla.
- Android / iOS: rodoma, kurios funkcijos dar nepalaikomos; XLSX skaitymas, DOCX -> PDF generavimas, SMTP siuntimas ir sugeneruotų aplankų atidarymas ten dar neišbaigti.

## Naudingi dokumentai
- `FEATURES.md` - produkto srautas ir techninės pastabos
- `EMAIL.md` - SMTP ir siuntimo elgsena
- `PDF_GEN.md` - DOCX -> PDF pipeline JVM platformoje
- `docs/architecture.md` - dabartinė repo architektūra

# D.U.K.
- Jeigu nepavyksta tam tikriems laiškams būti išsiųstiems, jie bus išsaugomi ir bus galima pamėginti pakartotina išsiųsti
