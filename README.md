# Sertifikatų generatorius

## Kaip naudotis
1. Pasirinkite XLSX failą su registracijos duomenimis.
2. Pasirinkite DOCX šabloną.
3. Užpildykite sertifikato laukus (ID, valandos, pavadinimas, lektorius ir kt.).
4. Spauskite „Konvertuoti į PDF“.
5. Failai bus sugeneruoti į `./pdf/<sertifikato_pavadinimas>`.

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

# D.U.K.
- Jeigu nepavyksta tam tikriems laiškams būti išsiųstiems, jie bus išsaugomi ir bus galima pamėginti pakartotina išsiųsti
