package dev.polurival.mexamen.data

import kotlinx.serialization.Serializable

@Serializable
data class Legend(
    val id: String,
    val title: String,
    val text: String,
    val questions: List<LegendQuestion>
)

@Serializable
data class LegendQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int
)

val legendCenoteZaci = Legend(
    id = "cenote-zaci",
    title = "El cenote Zací",
    text = """
        Durante el imperio de los Cupules y los Cocomes se fundó, en 1543, la ciudad maya de Zací (gavilán blanco) en lo que en la actualidad es la ciudad de Valladolid, Yucatán. Dentro de la ciudad se encontraba este cenote, y sobre su bóveda se encontraba la vivienda de la curandera y hechicera del pueblo. Esta curandera y hechicera era la jefa (patrona) de la familia Cocom; una anciana poderosa e influyente.

        Desde siempre, habían existido en Zací dos familias que se disputaban permanentemente el poder. El cacique Halach-Huinic (el hombre verdadero) era el que gobernaba en ese tiempo, y tenía un hijo llamado Hul-Kin (rayo de sol), perteneciente a la familia de los Cupules. La nieta de la hechicera se había quedado huérfana al nacer, y por ello la abuela la quería doblemente, ya que en ella cifraba el amor a su hija y a su propia nieta, la hermosa “Sac-Nicte” (flor blanca), quien le habría de alegrar la vida en su ancianidad.

        Al principio, Hul-Kin y Sac-Nicte parecían odiarse y peleaban constantemente. Sin embargo, al llegar a su juventud, surgió entre ellos una amistad que más tarde se convirtió en un apasionado amor, siendo más grande y avasallador el sentimiento de ella hacia al príncipe, pues Sac-Nicte era inocente y pura. Sin saberlo ni la hechicera ni el cacique, ellos se veían a escondidas, y al cabo de unos meses ella entregó su virginidad y pureza.

        A pesar de que guardaban en secreto su amor, el cacique se enteró y, enfurecido, envió a su hijo a un pueblo del sur de la península, con cuyo cacique ya había tratado una alianza: casar a Hul-Kin con la princesa de aquel lugar. Desde su separación, la alegría escapó del alma de Sac-Nicte. Sus ojos perdieron el brillo de la ilusión y entre llantos le contó a su amada abuela el secreto. Estaba embarazada y no quería vivir más. La abuela que era la hechicera más poderosa del pueblo le prometió que usaría todo su poder/poner todas sus fuerzas para traer de vuelta a su amado príncipe.

        Con el tiempo, Hul-Kin pareció olvidarse de Sac-Nicte. Su nuevo amor y prometida era igualmente bella. La hechicera quemaba copal y pedía ayuda a los dioses con todas sus fuerzas todas las noches de luna llena. Hacía sortilegios y suplicaba el regreso de Hul-Kin para devolverle la alegría al corazón de su nieta, quien, por las noches, como parte del ritual, se bañaba desnuda en el cenote, con los animales nocturnos como testigos mudos de su dolorosa esperanza.

        Un día, Sac-Nicte se enteró de que Hul-Kin se casaba. Tomó entonces una terrible decisión y la noche antes de la boda amarró una piedra a su larga cabellera y se arrojó a las aguas del cenote. En ese preciso momento, Hul-Kin sufrió un fuerte dolor en el pecho y tuvo un atroz presentimiento que lo obligó a regresar a Zací esa misma noche para averiguar qué había ocurrido. Al llegar, encontró a la hechicera llorando y maldiciendo enloquecida. Al enterarse de todo, Hul-Kin se arrojó al cenote para morir con su verdadero amor: Sac-Nicte.

        La hechicera se acercó al borde de la bóveda y, tirando una flor blanca, gritó sollozando: “Sac-Nicte, ¡te he cumplido! Te he traído a Hul-Kin, y ¡estará contigo para siempre!”. Finalmente, en un arranque de ira, la hechicera puso una maldición en el cenote: cada año, cuando el manto verde del agua se tornara oscuro, el cenote cobraría una vida en sacrificio por ese gran amor prohibido.
    """.trimIndent(),
    questions = listOf(
        LegendQuestion(1, "Identifica el sujeto en esta frase: “Esta curandera y hechicera era la jefa (patrona) de la familia Cocom.”", listOf("Curandera y hechicera", "Patrona", "Pueblo", "Cocom"), 0),
        LegendQuestion(2, "¿Cuáles son los nombres de las familias que tenían el poder del imperio?", listOf("Cupules y Cocomes", "Halach-Huinic y Sac-Nicte", "Hul-Kin y Halach-Huinic", "gavilán blanco y rayo de sol"), 0),
        LegendQuestion(3, "¿Qué significa \"Sac-Nicté\"?", listOf("rayo de sol", "gavilán blanco", "flor blanca", "hombre verdadero"), 2),
        LegendQuestion(4, "¿Qué le prometió la hechicera a su nieta?", listOf("traer de vuelta a Hul-Kin", "pedir ayuda a los dioses", "usar todo su poder/con todas sus fuerzas", "contar el secreto"), 0),
        LegendQuestion(5, "¿Qué amarró / ató Sac-Nicte a su larga cabellera?", listOf("una flor", "una piedra", "unas plantas", "nada"), 1),
        LegendQuestion(6, "¿Cuál fue la reacción de Hul-Kin cuando se enteró de que Sac-Nicte se había arrojado al cenote?", listOf("se arrojó al cenote", "no hizo nada", "sintió un fuerte dolor", "tiró una flor blanca"), 0)
    )
)

val legendDiaDeMuertos = Legend(
    id = "dia-de-muertos",
    title = "El Día de Muertos",
    text = """
        En México, el Día de Muertos es la festividad de raíces prehispánicas, combinada con la celebración católica del Día de Todos los Santos, la celebración busca rendir culto a los antepasados, pues se cree que los días 1 y 2 de noviembre, las almas de los fallecidos regresan para convivir con los vivos. Entre los lugares más emblemáticos en la celebración del Día de Muertos están Pátzcuaro, en Michoacán, y San Andrés Mixquic, en el Distrito Federal, así como la Huasteca Potosina, Huaquechula, Puebla y Ocotepec de Morelos, donde se ha logrado mantener esta tradición ancestral de convivencia entre vivos y muertos en casas, calles y cementerios.

        Al sur de la Ciudad de México, Mixquic, en la Delegación Tláhuac, tiene una de las celebraciones más impactantes, pues todo el pueblo participa en la fiesta para recibir a las almas de los difuntos. El 1 de noviembre llegan las ánimas de los niños y el día 2, las de los adultos, en medio de ofrendas, oraciones y el repique de las campanas. La alumbrada o noche de las velas es el evento más significativo de la celebración, ya que el 2 de noviembre, a partir de las 18:00 horas, los habitantes de Mixquic se encaminan hacia el panteón con velas, incienso y flores. En el cementerio, todas las tumbas son iluminadas con velas y se hace arder el incienso, lo que crea un ambiente místico, de comunión entre vivos y muertos. Durante esa noche, en medio de oraciones y cantos, los habitantes del pueblo de Mixquic permanecen al lado de las tumbas de sus deudos. Mientras transcurre la noche, la luz de las velas es la que alumbrará a las ánimas en su camino de regreso.
    """.trimIndent(),
    questions = listOf(
        LegendQuestion(1, "El Día de Muertos es:", listOf("Una celebración católica", "Una celebración prehispánica", "Una celebración que combina lo prehispánico con lo católico", "Una celebración colonial"), 2),
        LegendQuestion(2, "¿En qué delegación de la Ciudad de México hay una celebración impactante?", listOf("Coyoacán", "Tlalpan", "Venustiano Carranza", "Tláhuac"), 3),
        LegendQuestion(3, "¿Quién participa en la celebración del Día de Muertos en Mixquic?", listOf("Los parientes de los difuntos", "Los parientes y la iglesia local", "Todo el pueblo", "Los difuntos"), 2),
        LegendQuestion(4, "¿Cuál es el evento más significativo de la celebración?", listOf("La alumbrada o noche de las velas", "La visita de los muertos", "El repique de las campanas", "La ofrenda"), 0),
        LegendQuestion(5, "¿A quién/quiénes reciben en el 1 y 2 de noviembre?", listOf("a todo el pueblo", "a los visitantes del pueblo", "a las ánimas de los niños y adultos", "a los vivos"), 2),
        LegendQuestion(6, "¿Con qué encaminan los habitantes de Mixquic hacia el panteón?", listOf("Con velas, inciensos y flores", "Con las oraciones", "Con la música", "Con el repique de las campanas"), 0)
    )
)

val legendMaiz = Legend(
    id = "leyenda-maiz",
    title = "La leyenda del maíz",
    text = """
        Antes de la llegada del dios Quetzalcóatl, los aztecas se alimentaban de raíces y animales que cazaban, pero no podían comer maíz porque estaba escondido detrás de las montañas y no podían llegar hasta él. Los antiguos dioses habían intentado tiempo atrás separar estas altas montañas utilizando su fuerza, pero no lo consiguieron, así que los aztecas pidieron ayuda al dios Quetzalcóatl.

        Cuenta la leyenda que Quetzalcóatl no quiso emplear la fuerza, sino la inteligencia y la astucia, y se transformó en una hormiga negra. Decidió dirigirse a las montañas acompañado de una hormiga roja, dispuesto a conseguir el maíz para su pueblo. El camino estuvo lleno de dificultades, pero Quetzalcóatl las superó, pensando solamente en su pueblo y sus necesidades de alimentación. Hizo grandes esfuerzos y no se dio por vencido ante el cansancio y las dificultades.

        Tras mucho esfuerzo y sin perder el ánimo, Quetzalcóatl subió las montañas y cuando llegó a su destino, cogió entre sus mandíbulas un grano maduro de maíz e inició el duro regreso. Al llegar entregó el prometido grano de maíz a los hambrientos indígenas que plantaron la semilla, y desde entonces, tuvieron maíz para alimentarse. Los indios indígenas se convirtieron en un pueblo próspero y feliz para siempre. El preciado grano aumentó sus riquezas, y se volvieron más fuertes, construyeron ciudades, palacios, templos… Y desde entonces vivieron felices.

        Y a partir de ese momento, los aztecas veneraron al generoso Quetzalcóatl, el dios amigo de los hombres, el dios que les trajo el maíz. Jamás dejaron de adorarlo por haberles ayudado cuando más lo necesitaban.
    """.trimIndent(),
    questions = listOf(
        LegendQuestion(1, "¿De qué se alimentaban los aztecas antes de conseguir el maíz?", listOf("solo de animales", "de frutas y verduras", "de raíces y animales que cazaban", "de hongos"), 2),
        LegendQuestion(2, "¿Para qué Quetzalcóatl hizo el viaje a las montañas?", listOf("para conseguir el maíz para su pueblo", "para conseguir plantas", "para ayudar a los dioses", "para emplear la fuerza"), 0),
        LegendQuestion(3, "¿Cómo usó Quetzalcóatl su inteligencia y astucia para conseguir el maíz?", listOf("se convirtió en una ave", "subió las montañas", "pidió ayuda a los dioses", "se transformó en una hormiga negra"), 3),
        LegendQuestion(4, "¿Qué hizo Quetzalcóatl en la cima de la montaña?", listOf("cogió entre sus mandíbulas un grano maduro de maíz", "se dio por vencido", "empezó a pensar en su pueblo", "agarró el grano de maíz con sus manos"), 0),
        LegendQuestion(5, "¿Qué hicieron los indígenas después de que Quetzalcóatl les entregara el prometido grano de maíz?", listOf("compartieron la semilla con otros pueblos", "tuvieron maíz para alimentarse", "veneraron a Huitzilopochtli", "hicieron una celebración"), 1),
        LegendQuestion(6, "¿Por qué los azтеcas veneraron al dios Quetzalcóatl?", listOf("porque provocó la lluvia", "porque les trajo el maíz", "porque los defendió de sus enemigos", "porque les ayudó a construir ciudades, palacios, templos"), 1)
    )
)

val legendTequila = Legend(
    id = "leyenda-tequila",
    title = "Mayahuel o la leyenda del Tequila",
    text = """
        Cuenta la leyenda que una joven y bella diosa llamada Mayahuel, que vivía encerrada y lejos de los demás dioses debido que su abuela la cuidaba celosamente, tenía en su poder una planta mágica que daría alegría y otros dones al ser humano. Los dioses encomendaron a Quetzalcóatl la misión de poseer dicha planta para que se compartiera con los seres humanos. Astuto, este Dios con sus suaves palabras la convenció, sin importar el riesgo de la furia de la abuela. 

        En la huida, los dioses se enamoraron y decidieron vivir su amor eterno, sin embargo, dicha felicidad terminó cuando se dieron cuenta que los hermanos de Mayahuel los perseguían y para no ser encontrados, Mayahuel decidió que tanto ella como Quetzalcóatl tomarán la forma de su planta mágica. Sus hermanos solo vieron plantas y piedras, pero identificaron una planta diferente reconociendo a su hermana a la que cruelmente destrozaron. 

        Quetzalcóatl, que se había salvado, recobró su forma, tomó los restos de su amada y cuidadosamente la sembró, regándola todos los días con su llanto. Con el tiempo resurgió la planta mágica, pero Mayahuel no pudo recobrar su forma, quedando para siempre en el sagrado Maguey o Agave. 

        ¿Qué te pareció esta historia?... Efectivamente, es un gran regalo, ya que podemos adquirir Aguamiel, Tequila, Mezcal y de su penca se obtienen fibras para hacer telas, papel, entre otros.
    """.trimIndent(),
    questions = listOf(
        LegendQuestion(1, "¿Qué tenía en su poder la diosa Mayahuel?", listOf("una planta medicinal", "el don de la alegría", "una planta mágica", "una bebida maravillosa"), 2),
        LegendQuestion(2, "¿Cuál fue la misión que los dioses encomendaron a Quetzalcóatl?", listOf("enamorar a Mayahuel", "compartir la planta con los seres humanos", "esconderse de los hermanos de Mayahuel", "crear una bebida ritual"), 1),
        LegendQuestion(3, "¿Qué hicieron Mayahuel y Quetzalcóatl para no ser encontrados en su huida?", listOf("Se enamoraron y se escondieron", "se convertieron en una flor amarilla", "decidieron vivir su amor eterno", "tomaron la forma de la planta mágica"), 3),
        LegendQuestion(4, "¿Qué le hicieron los hermanos a Mayahuel?", listOf("la regresaron con su abuela", "la volvieron a encerrar", "la destrozaron", "la perdonaron"), 2),
        LegendQuestion(5, "¿Qué le pasó al final a Mayahuel?", listOf("recobró su forma original", "se quedó para siempre en el maguey", "vivió con Quetzalcóatl", "se convirtió en agua"), 1),
        LegendQuestion(6, "\"Astuto\" en este párrafo significa:", listOf("engañoso, él que sabe convencer con sus palabras", "ingenuo", "descuidado", "suave"), 0)
    )
)

val legendVolcanes = Legend(
    id = "leyenda-volcanes",
    title = "La leyenda de los volcanes",
    text = """
        Hubo una vez, un emperador muy rico y poderoso que tenía una hermosa hija llamada Iztaccíhuatl, la princesa estaba enamorada de Popocatépetl un joven guerrero, inteligente y valeroso. El emperador veía con agrado el matrimonio de su hija con el joven guerrero. Aquella era una época de grandes batallas entre los diferentes pueblos que luchaban por controlar el Valle de México, y cuando Iztaccíhuatl y Popocatépetl iban a celebrar su boda, los ejércitos enemigos declararon la guerra al imperio, así que como buen guerrero, Popocatépetl tuvo que cumplir su misión. 

        El emperador reunió a sus guerreros y le confió la misión de dirigirlos en los combates. Así partió ilusionado en cumplir su encargo lo más pronto posible para regresar a desposar a la bella Iztaccíhuatl, quién paciente esperaba la hora de que su amado llegará victorioso y poder vivir juntos por siempre. Tras varios meses de combate, Popocatépetl logró vencer a sus enemigos, pero antes de que el emperador supiera de la victoria, unos guerreros envidiosos le mintieron anunciándole que éste había muerto en combate. 

        Iztaccíhuatl escuchó la noticia falsa y lloró amargamente. La princesa dejó de comer y cayó en un sueño profundo, sin que nadie lograra despertarla. Cuando Popocatépetl regresó victorioso, supo lo que le había sucedido a su amada, la cargó en sus brazos, tomó una antorcha y salió del palacio y nadie volvió a verlos. Después de varios días, todas las personas del Valle de México se asombraron al ver dos montañas muy altas que habían surgido de la tierra. Se trataba de dos volcanes. Cuando el emperador los vio, dijo a su pueblo: “Iztaccíhuatl y Popocatépetl murieron de tristeza porque no podían vivir el uno sin el otro. El amor los ha transformado en volcanes y su corazón fiel arderá como una flama para siempre”.
    """.trimIndent(),
    questions = listOf(
        LegendQuestion(1, "En la época en que Iztaccihuatl y Popocatépetl estaban enamorados, en el Valle de México:", listOf("había gran prosperidad", "era una época de grandes batallas", "el imperio azteca estaba en su apogeo", "se esperaba la llegada de los españoles"), 1),
        LegendQuestion(2, "La misión que el emperador le confió a Popocatépetl fue:", listOf("casarse con Iztaccíhuatl", "ser el soldado más valiente en la guerra", "dirigir a los guerreros en los combates", "salir victorioso del combate"), 2),
        LegendQuestion(3, "Tras varios meses de combate:", listOf("Popocatépetl no lograba concluir el combate aunque iba ganando", "le pidió al emperador que lo dejara regresar para casarse con Iztaccíhuatl", "tenía la sospecha de que algunos de los guerrero lo traicionarían", "logró vencer a sus enemigos"), 3),
        LegendQuestion(4, "Cuando Popocatépetl regresó victorioso:", listOf("el emperador lo recibió con honores", "venía muy herido del combate", "escuchó noticias falsas", "supo lo que le sucedió a su amada"), 3),
        LegendQuestion(5, "La leyenda se trata:", listOf("de la gran batalla que libró Popocatépetl", "de un gran amor frustrado", "del origen de los volcanes", "de la intriga y el engaño"), 2),
        LegendQuestion(6, "¿Cómo se llamaba la princesa de la leyenda?", listOf("Iztaccíhuatl", "Mayahuel", "Xóchitl", "Sac-Nicte"), 0)
    )
)

val legendMayab = Legend(
    id = "leyenda-mayab",
    title = "El Mayab, la tierra del faisán y del venado",
    text = """
        Fue Itzamná quien creó todo lo que podemos ver en la tierra, la hizo bien bella para los humanos, pero dejó el Mayab a los mayas que eran los herederos de dicha tierra, les dio todo lo que necesitaban para sembrar y ser muy felices, también dejó a su lado a 3 animales que los cuidarían y estarían rondando este sitio de felicidad. El venado por la tierra, el faisán por los aires y la serpiente cascabel con su música serían los encargados de preservar la naturaleza junto con el hombre. 

        Todos eran felices en este lugar, la serpiente hacia música todo el día, el venado andaba de aquí para allá y el faisán volaba por todas las copas de los árboles. El día llegó en el que los chilam, o sea los adivinos mayas, tuvieron visiones terribles del futuro y le avisaron al hombre maya que tenga cuidado porque vendrían hombres de horrible corazón para llevarse sus tierras. Los mayas preguntaron si finalmente las tendrían y ellos asintieron que así sería. 

        La serpiente dejó de hacer música y solamente la empezó a hacer en el momento de atacar, el venado lloró tanto que se hicieron lagunas donde no las había y desde entonces siempre tiene una mirada triste, mientras que el faisán dejó de volar tanto para que no lo vean en los cielos. Los chilam también le dijeron al hombre maya que recuperarían sus tierras, pero para esto debían pelear en el momento exacto en donde el faisán se vea con toda su sombra en el Mayab, la serpiente haga una música diferente que se oiga en todas partes sin detenerse y el venado corra tan rápido que recorra todo el lugar en muy poco tiempo. 

        Ése fue el anuncio del adivino, pero el día aún no llega. Mientras tanto, los tres animales se preparan para estar listos. Así, el faisán alisa sus alas, el venado afila sus pezuñas y la serpiente frota sus cascabeles. Sólo esperan el momento de ser los mensajeros que reúnan a los mayas para recobrar El Mayab.
    """.trimIndent(),
    questions = listOf(
        LegendQuestion(1, "¿Por qué Itzamna dejó el Mayab a los mayas?", listOf("porque eran los herederos de dicha tierra", "porque eran los elegidos", "porque querían sembrar", "porque se lo pidieron"), 0),
        LegendQuestion(2, "¿A quiénes dejó Itzamná en la tierra nueva al lado de los mayas?", listOf("al venado, al faisán y al águila", "al faisán, al venado y a la serpiente", "a tres hormigas", "a la serpiente de cascabel y a unos jaguares"), 1),
        LegendQuestion(3, "¿Quiénes podrían ver visiones terribles del futuro?", listOf("todos los mayas", "los dioses", "los animales", "los chilam"), 3),
        LegendQuestion(4, "¿Cómo aparecieron las lagunas?", listOf("el venado lloró demasiado", "empezó a llover mucho", "hubo una inundación fuerte", "se salieron los ríos"), 0),
        LegendQuestion(5, "¿Por qué el faisán dejó de volar tanto?", listOf("para que no lo vieran en los cielos", "para que no lo vean los mayas", "porque se puso triste", "para atacar a los enemigos"), 0),
        LegendQuestion(6, "¿Qué esperan los tres animales al final de la leyenda?", listOf("Ser los mensajeros para recobrar El Mayab", "Que Itzamná regrese", "Que los chilam se vayan", "Atacar a los hombres de horrible corazón"), 0)
    )
)

val legendMujerDormida = Legend(
    id = "mujer-dormida",
    title = "La Mujer Dormida y el cerро Popocatepetl",
    text = """
        Tonatiuh, el Dios Sol, vivía con su familia en el cielo allí donde no se conocía la oscuridad, ni la angustia. El hijo del Dios Sol era el príncipe Izcozauhqui a quien le encantaban los jardines. Un día el príncipe oyó hablar de los lindos jardines del señor Tonacatecuhtli así que curioso fue a conocerlos. Las plantas allí parecían más verdes y los prados frescos y cubiertos de rocío. Al descubrir una laguna resplandeciente se acercó a verla y allí se encontró con una mujer que salía de las aguas ataviada con vestidos de plata. Se enamoraron de inmediato ante el beneplácito de los dioses. 

        Pasaban el tiempo juntos, recorrían un cielo y otro. Pero los dioses les prohibieron ir más allá del cielo. Los enamorados conocían el firmamento. La curiosidad por saber qué había bajo el cielo hizo que descendieran a conocer la tierra. Allí la vida era diferente. El sol no brillaba todo el tiempo, sino que descansaba por las noches. Había más colores, texturas, sonidos y animales que en todos los cielos recorridos. Los príncipes, al descubrir que la tierra era más hermosa que los paraísos celestiales decidieron quedarse a vivir en ella para siempre. El lugar escogido para su morada estaba cerca de un lago, al lado de valles y montañas. 

        Los dioses, furiosos por la desobediencia de la pareja, decidieron un castigo. La princesa enfermó repentinamente, fueron vanos los esfuerzos de Izcozauhqui por aliviarla. La mujer supo que esa era la sanción de los dioses. La princesa, antes de morir, le pidió a Izcozauhqui que la llevara a una montaña con el fin de estar junto a las nubes, para que, cuando él regresara con su padre, pudiera verla más cerca desde el cielo. Fueron sus últimas palabras, después se quedó quieta y blanca como la nieve. 

        El príncipe caminó días y noches hasta llegar a la cima de la montaña. Encendió una antorcha cerca de ella, la veló, como si la princesa durmiera. Izcozauhqui se quedó junto a ella, sin moverse, hasta morir. Ella se convirtió en la mujer dormida (volcán Iztaccíhuatl) y él en el cerro que humea (volcán Popocatépetl).
    """.trimIndent(),
    questions = listOf(
        LegendQuestion(1, "¿Cómo vivía Tonatiuh?", listOf("vivía donde se conocía la oscuridad", "vivía con su familia en el cielo", "vivía donde había angustia", "tenía dos hijos"), 1),
        LegendQuestion(2, "¿Por qué la vida en la tierra era diferente?", listOf("porque no era tan hermosa como los paraísos", "porque el sol brillaba todo el tiempo", "porque había más colores, texturas, sonidos", "porque había menos animales"), 2),
        LegendQuestion(3, "¿Por qué se enojaron los dioses con la pareja?", listOf("porque el príncipe Izcozauhqui se enamoró de la mujer de la laguna", "porque los enamorados pensaban que la tierra era mejor que el paraíso", "porque se quedaron en la tierra", "por la desobediencia de la pareja"), 3),
        LegendQuestion(4, "¿Cuál fue el último deseo de la princesa antes de morir?", listOf("que la llevara a una montaña", "regresar con sus padres", "que encendiera una antorcha", "que se convirtiera en una nube"), 0),
        LegendQuestion(5, "¿De qué se trata esta historia?", listOf("sobre el castigo de los dioses", "sobre el origen de la leyenda de los volcanes", "sobre el origen de la tierra", "sobre el origen de los hombres"), 1),
        LegendQuestion(6, "¿En qué se convirtió el príncipe Izcozauhqui?", listOf("En el cerro que humea (volcán Popocatépetl)", "En una laguna", "En el Dios Sol", "En una nube"), 0)
    )
)

val legendSumidero = Legend(
    id = "canon-sumidero",
    title = "Parque Nacional Cañón del Sumidero",
    text = """
        Imponente paisaje mexicano, conmemora 36 años como área natural protegida. Tiene un alto valor ecológico y cuenta con 21,789 hectáreas en cinco municipios del estado de Chiapas, a pocos kilómetros de su capital, Tuxtla Gutiérrez. Por su riqueza ecológica fue decretado Parque Nacional el 8 de diciembre de 1980. En 2004 fue clasificado como sitio Ramsar (humedal de importancia internacional). El cañón consiste en un imponente acantilado que se eleva sobre el cauce del río Grijalva. Sus muros, que alcanzan los más de 1000 metros de altura, se han convertido en uno de los paisajes más impresionantes de México, nominado desde hace tiempo para ser una de las Siete Nuevas Maravillas Naturales. La luz y el calor sobre las paredes del acantilado, así como sus abundantes escurrimientos han propiciado el surgimiento de varios microclimas en la vegetación y fauna en diferentes alturas del cañón. Algunas de sus especies endémicas (exclusivas de esta zona) son la salamandra Lengua Hongueada, la cuija Mexicana, el chupaflor Canelo, el sapo Jaspeado, la mojarra de Chiapa de Corzo y el encino Enano. El parque es frecuentado por decenas de miles de visitantes que gustan de practicar la bicicleta de montaña, el nado, el kayak y el rapel. Dentro de la zona se encuentra la presa hidroeléctrica de Chicoasén, una de las mayores obras mexicanas de ingeniería hidráulica, con un embalse de 32 kilómetros de largo que abarca la totalidad del cañón.
    """.trimIndent(),
    questions = listOf(
        LegendQuestion(1, "¿Por qué el Cañón del Sumidero fue decretado como Parque Nacional?", listOf("porque es una de las 7 maravillas del mundo", "por ser un lugar turístico popular", "por su riqueza ecológica", "por la decisión de la UNESCO"), 2),
        LegendQuestion(2, "Fue decretado como Parque Nacional …", listOf("8 de diciembre de 1980", "8 de noviembre de 2004", "20 de abril de 1980", "04 de diciembre de 1982"), 0),
        LegendQuestion(3, "¿Cuáles de estas especias son endémicas de la zona?", listOf("salamandras, cuijas, sapos", "mojarras, osos, serpientes", "jaguares, chupaflor, abejas", "encinos, iguanas, xoloitzcuintles"), 0),
        LegendQuestion(4, "¿Qué practican los visitantes?", listOf("la bicicleta de montaña, el nado, el kayak y el rapel", "paracaidísmo", "juegos deportivos", "la bicicleta de montaña y caminatas"), 0),
        LegendQuestion(5, "¿Qué fue clasificado como una obra de ingeniería dentro de esta zona?", listOf("el río Grijalva", "la presa hidroeléctrica de Chicoasén", "Tuxtla Gutiérrez", "balcón natural del Sumidero"), 1),
        LegendQuestion(6, "¿Qué es el sitio Ramsar mencionado en el texto?", listOf("Un humedal de importancia internacional", "Un parque nacional", "Una presa hidroeléctrica", "Una especie endémica"), 0)
    )
)

val legendCempasuchil = Legend(
    id = "leyenda-cempasuchil",
    title = "El día de muertos y la leyenda de la flor de cempasúchil",
    text = """
        En México una de las celebraciones más importantes es el Día de Muertos, en donde la flor de cempasúchil es un elemento fundamental. Cuenta la leyenda que hace mucho tiempo existieron un par de niños que se conocieron desde su nacimiento, la niña se llamaba Xóchitl y el niño Huitzilin. Ambos compartieron infancia, crecieron juntos y al final su amistad se convirtió en un dulce y tierno amor juvenil. Tanto era su cariño que un día decidieron subir a lo alto de una colina en donde el sol deslumbraba con particular fuerza, pues se sabía que allí moraba el Dios del sol. Hicieron todo ese largo camino sólo para pedirle a Tonatiuh que les diera su bendición y cuidado para poder seguir amándose. 

        El Dios del sol al verlos tan enamorados, bendijo su amor y aprobó su unión. Desafortunadamente la tragedia llegó a ellos de forma inesperada cuando Huitzilin fue llamado a participar en una batalla para defender a su pueblo, y fue así como se separaron para que él marchara a la guerra. Después de algún tiempo, Xóchitl se enteró que su amado había fallecido en el campo de batalla. Su dolor fue tan grande que rogó con todas sus fuerzas a Tonatiuh que le permitiera unirse a él en la eternidad. 

        Este, al verla tan afligida, decidió convertirla en una hermosa flor, así que lanzó un rayo dorado sobre ella, y en efecto, creció de la tierra un bello y tierno botón, sin embargo, este permaneció cerrado durante mucho tiempo. Un buen día un colibrí atraído por el aroma inconfundible de esta flor llegó hasta ella y se posó sobre sus hojas. Inmediatamente, la flor se abrió y mostró su hermoso color amarillo, radiante como el sol mismo, era la flor de cempasúchil, la flor de veinte pétalos, que había reconocido a su amado Huitzilin, el cual había tomado forma de colibrí para poder visitarla. 

        Así, la leyenda dice que mientras exista la flor de cempasúchil y haya colibríes en los campos, el amor de Huitzilin y Xóchitl perdurará por siempre. En el Día de Muertos, se cree el aroma de la flor de cempasúchil guía a las almas de los difuntos en el camino hacia la ofrenda que les espera en el mundo de los vivos. Esta es una clara referencia a que el aroma de esta flor guía con amor a las personas que se fueron, y que esperamos regresen a visitarnos año con año, y así reencontrarnos con nuestros seres queridos, tal como lo hicieron Xóchitl y Huitzilin.
    """.trimIndent(),
    questions = listOf(
        LegendQuestion(1, "Según el texto el elemento fundamental de Día de Muertos es:", listOf("la flor de nochebuena", "la flor de cempasúchil", "el pan de muerto", "la piñata"), 1),
        LegendQuestion(2, "Se sabía que en lo alto de la colina...", listOf("allí moraba el Dios del sol", "se juntaban los dioses", "había algo extraño", "crecía la flor de cempasúchil"), 0),
        LegendQuestion(3, "Xóchitl y Huitzilin hicieron un largo camino...", listOf("para comprobar su amor", "para hacer una ofrenda a los dioses", "para pedirle ayuda al dios del sol", "para que Tonatiuh les diera su bendición y cuidado"), 3),
        LegendQuestion(4, "¿En qué se convirtieron Xóchitl y Huitzilin?", listOf("En un árbol y un jaguar", "En una rosa y un águila", "En una flor de cempasúchil y un colibrí", "En una piedra y una mariposa"), 2),
        LegendQuestion(5, "Se cree que la flor de cempasúchil ayuda...", listOf("a guiar con amor a las personas que se fueron", "a conectarse con la familia", "a hacer una ofrenda a Tonatiuh", "a celebrar el Día de Muertos"), 0),
        LegendQuestion(6, "¿Qué forma tomó Huitzilin para visitar a Xóchitl?", listOf("Colibrí", "Águila", "Jaguar", "Flor"), 0)
    )
)

val legendNochebuena = Legend(
    id = "leyenda-nochebuena",
    title = "La leyenda de la flor de nochebuena",
    text = """
        Una historia de la época de la Conquista, cuenta que en un pueblo de Guerrero habitaba una niña muy humilde que cada año esperaba con ansias la llegada de la Navidad. Pero su padre se había quedado sin trabajo ese año, y no tenía dinero para ofrendar frutas, dulces o juguetes al niño Jesús durante la misa de Nochebuena, por lo que se encontraba muy preocupada y triste. 

        Al llegar la hora de la misa, caminó hacia la iglesia junto con sus padres. Sin embargo, al ver que todos los niños cargaban las mejores ofrendas, se llenó de vergüenza por ir con las manos vacías. Corrió hacia los arbustos y se puso a llorar. De pronto, escuchó una voz de un niño que le dijo: 

        — Ya no llores. Arranca esas plantas verdes que están ahí y llévalas al altar del niño Jesús. 

        Decidió hacerle caso al niño y agarró un manojo de aquellas plantas silvestres. Aún llena de pena, por pensar que su ofrenda era simple y lamentable, caminó lentamente hacia el interior de la iglesia. Al llegar, los parroquianos lanzaron una expresión de sorpresa pues eran las flores más hermosas que habían visto sus ojos. La planta había cambiado sus hojas de color milagrosamente y ahora resplandecían un color rojo intenso y tenían forma de estrellas. 

        —¡Feliz Navidad, niño Jesús!— dijo la niña al depositar su maravillosa ofrenda frente al altar. Desde ese día, la flor de nochebuena, creció en todas partes de México durante la Navidad.
    """.trimIndent(),
    questions = listOf(
        LegendQuestion(1, "¿Por qué estaba triste y preocupada la niña?", listOf("porque su padre no tenía trabajo", "porque su familia era pobre", "Porque la niña no tenía dinero para ofrendar frutas, dulces o juguetes al niño Jesús", "porque ya venía la misa de nochebuena"), 2),
        LegendQuestion(2, "¿Qué escuchó de pronto la niña?", listOf("la voz de su padre", "la voz de un niño", "su propia voz", "el viento"), 1),
        LegendQuestion(3, "¿Qué le pidió el niño?", listOf("que llevara ofrendas a la misa", "que arrancara unas plantas verdes", "que llevara juguetes", "que llevara una buena ofrenda a la misa"), 1),
        LegendQuestion(4, "¿Qué le pasó al manojo de plantas silvestres en la iglesia?", listOf("cambiaron de color y forma", "se convirtieron en rosas", "tomaron un aroma especial", "tomaron un color dorado"), 0),
        LegendQuestion(5, "¿Dónde crece la flor de nochebuena?", listOf("en la Ciudad de México", "en el cerro de Tepeyac", "en todo México", "en el Valle de México"), 2),
        LegendQuestion(6, "¿Qué forma tenían las flores rojas en el altar?", listOf("De estrellas", "De campanas", "De rosas", "De cruces"), 0)
    )
)

val allLegends = listOf(
    legendCenoteZaci,
    legendDiaDeMuertos,
    legendMaiz,
    legendTequila,
    legendVolcanes,
    legendMayab,
    legendMujerDormida,
    legendSumidero,
    legendCempasuchil,
    legendNochebuena
)
