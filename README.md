# Sertifikatų generatorius

## Kaip naudotis
1. Pasirinkite XLSX failą su registracijos duomenimis.
2. Pasirinkite DOCX šabloną.
3. Pasirinkite išvesties aplanką.
4. Užpildykite sertifikato laukus (ID, valandos, pavadinimas, lektorius ir kt.).
5. Spauskite „Konvertuoti į PDF“ (šiuo metu sugeneruojami DOCX failai).

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
