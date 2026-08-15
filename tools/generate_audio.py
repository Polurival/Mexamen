import os
import json
import re
import textwrap
from google.cloud import texttospeech_v1beta1 as texttospeech
from google.api_core.exceptions import InvalidArgument

# Setup:
# 1. source venv/bin/activate
# 2. export GOOGLE_APPLICATION_CREDENTIALS="/home/polurival/AndroidStudioProjects/Mexamen/composeApp/mexamen-34f04829d412.json"

LEGENDS = [
    {
        "id": "cenote-zaci",
        "title": "El cenote Zací",
        "text": """
            Durante el imperio de los Cupules y los Cocomes se fundó, en 1543, la ciudad maya de Zací (gavilán blanco) en lo que en la actualidad es la ciudad de Valladolid, Yucatán. Dentro de la ciudad se encontraba este cenote, y sobre su bóveda se encontraba la vivienda de la curandera y hechicera del pueblo. Esta curandera y hechicera era la jefa (patrona) de la familia Cocom; una anciana poderosa e influyente.

            Desde siempre, habían existido en Zací dos familias que se disputaban permanentemente el poder. El cacique Halach-Huinic (el hombre verdadero) era el que gobernaba en ese tiempo, y tenía un hijo llamado Hul-Kin (rayo de sol), perteneciente a la familia de los Cupules. La nieta de la hechicera se había quedado huérfana al nacer, y por ello la abuela la quería doblemente, ya que en ella cifraba el amor a su hija y a su propia nieta, la hermosa “Sac-Nicte” (flor blanca), quien le habría de alegrar la vida en su ancianidad.

            Al principio, Hul-Kin y Sac-Nicte parecían odiarse y peleaban constantemente. Sin embargo, al llegar a su juventud, surgió entre ellos una amistad que más tarde se convirtió en un apasionado amor, siendo más grande y avasallador el sentimiento de ella hacia al príncipe, pues Sac-Nicte era inocente y pura. Sin saberlo ni la hechicera ni el cacique, ellos se veían a escondidas, y al cabo de unos meses ella entregó su virginidad y pureza.

            A pesar de que guardaban en secreto su amor, el cacique se enteró y, enfurecido, envió a su hijo a un pueblo del sur de la península, con cuyo cacique ya había tratado una alianza: casar a Hul-Kin con la princesa de aquel lugar. Desde su separación, la alegría escapó del alma de Sac-Nicte. Sus ojos perdieron el brillo de la ilusión y entre llantos le contó a su amada abuela el secreto. Estaba embarazada y no quería vivir más. La abuela que era la hechicera más poderosa del pueblo le prometió que usaría todo su poder para traer de vuelta a su amado príncipe.

            Con el tiempo, Hul-Kin pareció olvidarse de Sac-Nicte. Su nuevo amor y prometida era igualmente bella. La hechicera quemaba copal y pedía ayuda a los dioses con todas sus fuerzas todas las noches de luna llena. Hacía sortilegios y suplicaba el regreso de Hul-Kin para devolverle la alegría al corazón de su nieta, quien, por las noches, como parte del ritual, se bañaba desnuda en el cenote, con los animales nocturnos como testigos mudos de su dolorosa esperanza.

            Un día, Sac-Nicte se enteró de que Hul-Kin se casaba. Tomó entonces una terrible decisión y la noche antes de la boda amarró una piedra a su larga cabellera y se arrojó a las aguas del cenote. En ese preciso momento, Hul-Kin sufrió un fuerte dolor en el pecho y tuvo un atroz presentimiento que lo obligó a regresar a Zací esa misma noche para averiguar qué había ocurrido. Al llegar, encontró a la hechicera llorando y maldiciendo enloquecida. Al enterarse de todo, Hul-Kin se arrojó al cenote para morir con su verdadero amor: Sac-Nicte.

            La hechicera se acercó al borde de la bóveda and, tirando una flor blanca, gritó sollozando: “Sac-Nicte, ¡te he cumplido! Te he traído a Hul-Kin, y ¡estará contigo para siempre!”. Finalmente, en un arranque de ira, la hechicera puso una maldición en el cenote: cada año, cuando el manto verde del agua se tornara oscuro, el cenote cobraría una vida en sacrificio por ese gran amor prohibido."""
    },
    {
        "id": "dia-de-muertos",
        "title": "El Día de Muertos",
        "text": """
            En México, el Día de Muertos es la festividad de raíces prehispánicas, combinada con la celebración católica del Día de Todos los Santos, la celebración busca rendir culto a los antepasados, pues se cree que los días 1 y 2 de noviembre, las almas de los fallecidos regresan para convivir con los vivos. Entre los lugares más emblemáticos en la celebración del Día de Muertos están Pátzcuaro, en Michoacán, y San Andrés Mixquic, en el Distrito Federal, así como la Huasteca Potosina, Huaquechula, Puebla y Ocotepec de Morelos, donde se ha logrado mantener esta tradición ancestral de convivencia entre vivos y muertos en casas, calles y cementerios.

            Al sur de la Ciudad de México, Mixquic, en la Delegación Tláhuac, tiene una de las celebraciones más impactantes, pues todo el pueblo participa en la fiesta para recibir a las almas de los difuntos. El 1 de noviembre llegan las ánimas de los niños y el día 2, las de los adultos, en medio de ofrendas, oraciones y el repique de las campanas. La alumbrada o noche de las velas es el evento más significativo de la celebración, ya que el 2 de noviembre, a partir de las 18:00 horas, los habitantes de Mixquic se encaminan hacia el panteón con velas, incienso y flores. En el cementerio, todas las tumbas son iluminadas con velas y se hace arder el incienso, lo que crea un ambiente místico, de comunión entre vivos y muertos. Durante esa noche, en medio de oraciones y cantos, los habitantes del pueblo de Mixquic permanecen al lado de las tumbas de sus deudos. Mientras transcurre la noche, la luz de las velas es la que alumbrará a las ánimas en su camino de regreso."""
    },
    {
        "id": "leyenda-maiz",
        "title": "La leyenda del maíz",
        "text": """
            Antes de la llegada del dios Quetzalcóatl, los aztecas se alimentaban de raíces y animales que cazaban, pero no podían comer maíz porque estaba escondido detrás de las montañas y no podían llegar hasta él. Los antiguos dioses habían intentado tiempo atrás separar estas altas montañas utilizando su fuerza, pero no lo consiguieron, así que los aztecas pidieron ayuda al dios Quetzalcóatl.

            Cuenta la leyenda que Quetzalcóatl no quiso emplear la fuerza, sino la inteligencia y la astucia, y se transformó en una hormiga negra. Decidió dirigirse a las montañas acompañado de una hormiga roja, dispuesto a conseguir el maíz para su pueblo. El camino estuvo lleno de dificultades, pero Quetzalcóatl las superó, pensando solamente en su pueblo and sus necesidades de alimentación. Hizo grandes esfuerzos y no se dio por vencido ante el cansancio and las dificultades.

            Tras mucho esfuerzo y sin perder el ánimo, Quetzalcóatl subió las montañas and cuando llegó a su destino, cogió entre sus mandíbulas un grano maduro de maíz e inició el duro regreso. Al llegar entregó el prometido grano de maíz a los hambrientos indígenas que plantaron la semilla, and desde entonces, tuvieron maíz para alimentarse. Los indios indígenas se convirtieron en un pueblo próspero y feliz para siempre. El preciado grano aumentó sus riquezas, and se volvieron más fuertes, construyeron ciudades, palacios, templos… Y desde entonces vivieron felices.

            Y a partir de ese momento, los aztecas veneraron al generoso Quetzalcóatl, el dios amigo de los hombres, el dios que les trajo el maíz. Jamás dejaron de adorarlo por haberles ayudado cuando más lo necesitaban."""
    },
    {
        "id": "leyenda-tequila",
        "title": "Mayahuel o la leyenda del Tequila",
        "text": """
            Cuenta la leyenda que una joven y bella diosa llamada Mayahuel, que vivía encerrada y lejos de los demás dioses debido que su abuela la cuidaba celosamente, tenía en su poder una planta mágica que daría alegría y otros dones al ser humano. Los dioses encomendaron a Quetzalcóatl la misión de poseer dicha planta para que se compartiera con los seres humanos. Astuto, este Dios con sus suaves palabras la convenció, sin importar el riesgo de la furia de la abuela.

            En la huida, los dioses se enamoraron y decidieron vivir su amor eterno, sin embargo, dicha felicidad terminó cuando se dieron cuenta que los hermanos de Mayahuel los perseguían y para no ser encontrados, Mayahuel decidió que tanto ella como Quetzalcóatl tomarán la forma de su planta mágica. Sus hermanos solo vieron plantas y piedras, pero identificaron una planta diferente reconociendo a su hermana a la que cruelmente destrozaron.

            Quetzalcóatl, que se había salvado, recobró su forma, tomó los restos de su amada y cuidadosamente la sembró, regándola todos los días con su llanto. Con el tiempo resurgió la planta mágica, pero Mayahuel no pudo recobrar su forma, quedando para siempre en el sagrado Maguey o Agave.

            ¿Qué te pareció esta historia?... Efectivamente, es un gran regalo, ya que podemos adquirir Aguamiel, Tequila, Mezcal y de su penca se obtienen fibras para hacer telas, papel, entre otros."""
    },
    {
        "id": "leyenda-volcanes",
        "title": "La leyenda de los volcanes",
        "text": """
            Hubo una vez, un emperador muy rico y poderoso que tenía una hermosa hija llamada Iztaccíhuatl, la princesa estaba enamorada de Popocatépetl un joven guerrero, inteligente y valeroso. El emperador veía con agrado el matrimonio de su hija con el joven guerrero. Aquella era una época de grandes batallas entre los diferentes pueblos que luchaban por controlar el Valle de México, and cuando Iztaccíhuatl y Popocatépetl iban a celebrar su boda, los ejércitos enemigos declararon la guerra al imperio, así que como buen guerrero, Popocatépetl tuvo que cumplir su misión.

            El emperador reunió a sus guerreros and le confió la misión de dirigirlos en los combates. Así partió ilusionado en cumplir su encargo lo más pronto posible para regresar a desposar a la bella Iztaccíhuatl, quién paciente esperaba la hora de que su amado llegará victorioso and poder vivir juntos por siempre. Tras varios meses de combate, Popocatépetl logró vencer a sus enemigos, pero antes de que el emperador supiera de la victoria, unos guerreros envidiosos le mintieron anunciándole que éste había muerto en combate.

            Iztaccíhuatl escuchó la noticia falsa and lloró amargamente. La princesa dejó de comer and cayó en un sueño profundo, sin que nadie lograra despertarla. Cuando Popocatépetl regresó victorioso, supo lo que le había sucedido a su amada, la cargó en sus brazos, tomó una antorcha and salió del palacio and nadie volvió a verlos. Después de varios días, todas las personas del Valle de México se asombraron al ver dos montañas muy altas que habían surgido de la tierra. Se trataba de dos volcanes. Cuando el emperador los vio, dijo a su pueblo: “Iztaccíhuatl y Popocatépetl murieron de tristeza porque no podían vivir el uno sin el otro. El amor los ha transformado en volcanes y su corazón fiel arderá como una flama para siempre”."""
    },
    {
        "id": "leyenda-mayab",
        "title": "El Mayab, la tierra del faisán y del venado",
        "text": """
            Fue Itzamná quien creó todo lo que podemos ver en la tierra, la hizo bien bella para los humanos, pero dejó el Mayab a los mayas que eran los herederos de dicha tierra, les dio todo lo que necesitaban para sembrar and ser muy felices, también dejó a su lado a 3 animales que los cuidarían and estarían rondando este sitio de felicidad. El venado por la tierra, el faisán por los aires and la serpiente cascabel con su música serían los encargados de preservar la naturaleza junto con el hombre.

            Todos eran felices in este lugar, la serpiente hacia música todo el día, el venado andaba de aquí para allá and el faisán volaba por todas las copas de los árboles. El día llegó in el que los chilam, o sea los adivinos mayas, tuvieron visiones terribles del futuro and le avisaron al hombre maya que tenga cuidado porque vendrían hombres de horrible corazón para llevarse sus tierras. Los mayas preguntaron si finalmente las tendrían and ellos asintieron que así sería.

            La serpiente dejó de hacer música and solamente la empezó a hacer in el momento de atacar, el venado lloró tanto que se hicieron lagunas donde no las había and desde entonces siempre tiene una mirada triste, mientras que el faisán dejó de volar tanto para que no lo vean in los cielos. Los chilam también le dijeron al hombre maya que recuperarían sus tierras, pero para esto debían pelear in el momento exacto in donde el faisán se vea con toda su sombra in el Mayab, la serpiente haga una música diferente que se oiga in todas partes sin detenerse and el venado corra tan rápido que recorra todo el lugar in muy poco tiempo.

            Ése fue el anuncio del adivino, pero el día aún no llega. Mientras tanto, los tres animales se preparan para estar listos. Así, el faisán alisa sus alas, el venado afila sus pezuñas and la serpiente frota sus cascabeles. Sólo esperan el momento de ser los mensajeros que reúnan a los mayas para recobrar El Mayab."""
    },
    {
        "id": "mujer-dormida",
        "title": "La Mujer Dormida y el cerро Popocatepetl",
        "text": """
            Tonatiuh, el Dios Sol, vivía con su familia in el cielo allí donde no se conocía la oscuridad, ni la angustia. El hijo del Dios Sol era el príncipe Izcozauhqui a quien le encantaban los jardines. Un día el príncipe oyó hablar de los lindos jardines del señor Tonacatecuhtli así que curioso fue a conocerlos. Las plantas allí parecían más verdes and los prados frescos and cubiertos de rocío. Al descubrir una laguna resplandeciente se acercó a verla and allí se encontró con una mujer que salía de las aguas ataviada con vestidos de plata. Se enamoraron de inmediato ante el beneplácito de los dioses.

            Pasaban el tiempo juntos, recorrían un cielo and otro. Pero los dioses les prohibieron ir más allá del cielo. Los enamorados conocían el firmamento. La curiosidad por saber qué había bajo el cielo hizo que descendieran a conocer la tierra. Allí la vida era diferente. El sol no brillaba todo el tiempo, sino que descansaba por las noches. Había más colores, texturas, sonidos and animales que in todos los cielos recorridos. Los príncipes, al descubrir que la tierra era más hermosa que los paraísos celestiales decidieron quedarse a vivir in ella para siempre. El lugar escogido para su morada estaba cerca de un lago, al lado de valles and montañas.

            Los dioses, furiosos por la desobediencia de la pareja, decidieron un castigo. La princesa enfermó repentinamente, fueron vanos los esfuerzos de Izcozauhqui por aliviarla. La mujer supo que esa era la sanción de los dioses. La princesa, antes de morir, le pidió a Izcozauhqui que la llevara a una montaña con el fin de estar junto a las nubes, para que, cuando él regresara con su padre, pudiera verla más cerca desde el cielo. Fueron sus últimas palabras, después se quedó quieta and blanca como la nieve.

            El príncipe caminó días and noches hasta llegar a la cima de la montaña. Encendió una antorcha cerca de ella, la veló, como si la princesa durmiera. Izcozauhqui se quedó junto a ella, sin moverse, hasta morir. Ella se convirtió in la mujer dormida (volcán Iztaccíhuatl) and él in el cerro que humea (volcán Popocatépetl)."""
    },
    {
        "id": "canon-sumidero",
        "title": "Parque Nacional Cañón del Sumidero",
        "text": """
            Imponente paisaje mexicano, conmemora 36 años como área natural protegida. Tiene un alto valor ecológico and cuenta con 21,789 hectáreas in cinco municipios del estado de Chiapas, a pocos kilómetros de su capital, Tuxtla Gutiérrez. Por su riqueza ecológica fue decretado Parque Nacional el 8 de diciembre de 1980. En 2004 fue clasificado como sitio Ramsar (humedal de importancia internacional). El cañón consiste in un imponente acantilado que se eleva sobre el cauce del río Grijalva. Sus muros, que alcanzan los más de 1000 metros de altura, se han convertido in uno de los paisajes más impresionantes de México, nominado desde hace tiempo para ser una de las Siete Nuevas Maravillas Naturales. La luz and el calor sobre las paredes del acantilado, así como sus abundantes escurrimientos han propiciado el surgimiento de varios microclimas in la vegetación and fauna in diferentes alturas del cañón. Algunas de sus especies endémicas (exclusivas de esta zona) son la salamandra Lengua Hongueada, la cuija Mexicana, el chupaflor Canelo, el sapo Jaspeado, la mojarra de Chiapa de Corzo and el encino Enano. El parque es frecuentado por decenas de miles de visitantes que gustan de practicar la bicicleta de montaña, el nado, el kayak and el rapel. Dentro de la zona se encuentra la presa hidroeléctrica de Chicoasén, una de las mayores obras mexicanas de ingeniería hidráulica, con un embalse de 32 kilómetros de largo que abarca la totalidad del cañón."""
    },
    {
        "id": "leyenda-cempasuchil",
        "title": "El día de muertos y la leyenda de la flor de cempasúchil",
        "text": """
            En México una de las celebraciones más importantes es el Día de Muertos, in donde la flor de cempasúchil es un elemento fundamental. Cuenta la leyenda que hace mucho tiempo existieron un par de niños que se conocieron desde su nacimiento, la niña se llamaba Xóchitl and el niño Huitzilin. Ambos compartieron infancia, crecieron juntos and al final su amistad se convirtió in un dulce and tierno amor juvenil. Tanto era su cariño que un día decidieron subir a lo alto de una colina in donde el sol deslumbraba con particular fuerza, pues se sabía que allí moraba el Dios del sol. Hicieron todo ese largo camino sólo para pedirle a Tonatiuh que les diera su bendición and cuidado para poder seguir amándose.

            El Dios del sol al verlos tan enamorados, bendijo su amor and aprobó su unión. Desafortunadamente la tragedia llegó a ellos de forma inesperada cuando Huitzilin fue llamado a participar in una batalla para defender a su pueblo, and fue así como se separaron para que él marchara a la guerra. Después de algún tiempo, Xóchitl se enteró que su amado había fallecido in el campo de batalla. Su dolor fue tan grande que rogó con todas sus fuerzas a Tonatiuh que le permitiera unirse a él in la eternidad.

            Este, al verla tan afligida, decidió convertirla in una hermosa flor, así que lanzó un rayo dorado sobre ella, and in efecto, creció de la tierra un bello and tierno botón, sin embargo, este permaneció cerrado durante mucho tiempo. Un buen día un colibrí atraído por el aroma inconfundible de esta flor llegó hasta ella and se posó sobre sus hojas. Inmediatamente, la flor se abrió and mostró su hermoso color amarillo, radiante como el sol mismo, era la flor de cempasúchil, la flor de veinte pétalos, que había reconocido a su amado Huitzilin, el cual había tomado forma de colibrí para poder visitarla.

            Así, la leyenda dice que mientras exista la flor de cempasúchil and haya colibríes in los campos, el amor de Huitzilin and Xóchitl perdurará por siempre. En el Día de Muertos, se cree el aroma de la flor de cempasúchil guía a las almas de los difuntos in el camino hacia la ofrenda que les espera in el mundo de los vivos. Esta is una clara referencia a que el aroma de esta flor guía con amor a las personas que se fueron, and que esperamos regresen a visitarnos año con año, and así reencontrarnos con nuestros seres queridos, tal como lo hicieron Xóchitl and Huitzilin."""
    },
    {
        "id": "leyenda-nochebuena",
        "title": "La leyenda de la flor de nochebuena",
        "text": """
            Una historia de la época de la Conquista, cuenta que in un pueblo de Guerrero habitaba una niña muy humilde que cada año esperaba con ansias la llegada de la Navidad. Pero su padre se había quedado sin trabajo ese año, and no tenía dinero para ofrendar frutas, dulces o juguetes al niño Jesús durante la misa de Nochebuena, por lo que se encontraba muy preocupada and triste.

            Al llegar la hora de la misa, caminó hacia la iglesia junto con sus padres. Sin embargo, al ver que todos los niños cargaban las mejores ofrendas, se llenó de vergüenza por ir con las manos vacías. Corrió hacia los arbustos and se puso a llorar. De pronto, escuchó una voz de un niño que le dijo:

            — Ya no llores. Arranca esas plantas verdes que están ahí and llévalas al altar del niño Jesús.

            Decidió hacerle caso al niño and agarró un manojo de aquellas plantas silvestres. Aún llena de pena, por pensar que su ofrenda era simple and lamentable, caminó lentamente hacia el interior de la iglesia. Al llegar, los parroquianos lanzaron una expresión de sorpresa pues eran las flores más hermosas que habían visto sus ojos. La planta había cambiado sus hojas de color milagrosamente and ahora resplandecían un color rojo intenso and tenían forma de estrellas.

            —¡Feliz Navidad, niño Jesús!— dijo la niña al depositar su maravillosa ofrenda frente al altar. Desde ese día, la flor de nochebuena, creció in todas partes de México durante la Navidad."""
    }
]

def clean_text(text):
    # Mimic Kotlin's trimIndent()
    dedented = textwrap.dedent(text)
    return dedented.strip()

def text_to_ssml(text):
    # Split text into tokens (words and whitespace/punctuation)
    tokens = re.findall(r'\S+|\s+', text)
    ssml = "<speak>"
    words_info = []
    current_offset = 0
    word_index = 0
    for token in tokens:
        if re.match(r'\S+', token):
            ssml += f'<mark name="{word_index}"/>{token}'
            words_info.append({
                "index": word_index,
                "start": current_offset,
                "end": current_offset + len(token)
            })
            word_index += 1
        else:
            ssml += token
        current_offset += len(token)
    ssml += "</speak>"
    return ssml, words_info

def generate_audio_and_metadata(legend_id, text, output_dir="audio"):
    client = texttospeech.TextToSpeechClient()

    text = clean_text(text) # CRITICAL: Ensure text matches App text exactly
    ssml, words_info = text_to_ssml(text)
    synthesis_input = texttospeech.SynthesisInput(ssml=ssml)

    # Preferred: es-MX-Neural2-A, Fallback: es-US-Neural2-A
    voices_to_try = [
        ("es-MX", "es-MX-Neural2-A"),
        ("es-US", "es-US-Neural2-A")
    ]

    response = None
    for lang, name in voices_to_try:
        try:
            print(f"Attempting voice: {name} for {legend_id}...")
            voice = texttospeech.VoiceSelectionParams(
                language_code=lang,
                name=name,
                ssml_gender=texttospeech.SsmlVoiceGender.FEMALE
            )

            audio_config = texttospeech.AudioConfig(
                audio_encoding=texttospeech.AudioEncoding.MP3,
                pitch=0,
                speaking_rate=0.95
            )

            request = texttospeech.SynthesizeSpeechRequest(
                input=synthesis_input,
                voice=voice,
                audio_config=audio_config,
                enable_time_pointing=[texttospeech.SynthesizeSpeechRequest.TimepointType.SSML_MARK]
            )
            response = client.synthesize_speech(request=request)
            if response:
                break
        except Exception as e:
            print(f"Voice {name} failed: {e}")
            continue

    if not response:
        print(f"FAILED {legend_id}")
        return

    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    # Save MP3
    mp3_filename = f"{output_dir}/{legend_id}.mp3"
    with open(mp3_filename, "wb") as out:
        out.write(response.audio_content)

    # Save JSON Timings
    timepoints = []
    for tp in response.timepoints:
        idx = int(tp.mark_name)
        if idx < len(words_info):
            info = words_info[idx]
            timepoints.append({
                "start": info["start"],
                "end": info["end"],
                "time_ms": int(tp.time_seconds * 1000)
            })

    json_filename = f"{output_dir}/{legend_id}.json"
    with open(json_filename, "w", encoding="utf-8") as out:
        json.dump(timepoints, out, ensure_ascii=False, indent=2)

    print(f'SUCCESS: Generated {mp3_filename} and {json_filename}')

if __name__ == "__main__":
    for legend in LEGENDS:
        generate_audio_and_metadata(legend['id'], legend['text'])
