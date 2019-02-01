(ns salava.extra.passport.ui.block
  (:require [salava.core.i18n :refer [t]]))


(defn ^:export footer []
  [:footer.footer
   [:div.footer-container
    [:p.text-muted
     "Copyright © 2015-2019 Open Badge Factory Oy | "
     [:a {:class "bottom-link" :href "/terms" }
      "Terms of Use"]
     " | "
     [:a {:class "bottom-link" :href "/privacy"}
      "Privacy Policy"]
     " | "
     [:a {:class "bottom-link" :href "mailto:contact@openbadgefactory.com?Subject=Contact%20request" }
      "contact@openbadgefactory.com"]]
    [:p.text-muted
     "Open Badge Factory ® and Open Badge Passport ® are registered trademarks"]]])


(defn ^:export terms_fr []
  [:div {:style {:padding "30px"}}
   [:div
    [:h1 {:style {:text-align "center"}} (clojure.string/upper-case "Conditions d’utilisation")]]
   [:div {:style {:text-align "center"}}
    [:p "Mis à jour le 29.1.2019 "]]
   [:div
    [:h2 "Informations générales sur l’utilisation du service Open Badge Passport"]
    [:p  "Le service Open Badge Passport (ci-après dénommé ” le service ”) est
     proposé par Open Badge Factory Oy (ci-après dénommé ” le prestataire de services ”).
     Le service fourni par le prestataire de services consiste en une plate-forme basée
     sur le cloud permettant aux bénéficiaires de badges ouverts de recevoir, partager,
     gérer et publier leurs badges.  Les conditions d’utilisation s’appliquent aux utilisateurs du service (ci-après dénommés ” l’utilisateur ” ou ” les utilisateurs ”). Le terme d’utilisateur correspond à deux niveaux d’utilisateur : 1) les utilisateurs qui utilisent le service à des fins personnelles ; 2) l’organisation du client, qui dispose de son propre environnement dans le service."]
    [:br]
    [:p "Dès lors qu’il utilise le service Open Badge Passport,
     l’utilisateur est réputé accepter les présentes conditions d’utilisation."]
    ]
   [:div
    [:h2 "Accès au service et limitations"]
    [:p "En créant un compte d’utilisateur dans le service, l’utilisateur déclare au prestataire de services qu’il est âgé d’au moins 13 ans et, s’il est âgé de moins de 18 ans,
     qu’il a l’autorisation de ses parents. "]
    [:p "Sous réserve de l’acceptation de l’utilisateur et du respect constant des présentes conditions d’utilisation, le prestataire de services accorde à l’utilisateur une licence limitée, non exclusive, incessible et révocable d’utilisation du service dans le seul but de recevoir, gérer, afficher, partager et publier des badges ouverts. L’utilisateur s’engage à n’utiliser le service dans aucun autre but. Le service ne saurait être utilisé d’aucune autre manière qu’à l’aide des interfaces spécifiées et dans le respect des instructions données et des présentes conditions d’utilisation communiquées par le prestataire de services.
     En dehors des cas spécialement autorisés dans les présentes conditions d’utilisation, l’utilisateur n’est pas autorisé à utiliser, copier, reproduire, republier, stocker, modifier, transférer, afficher, encoder, transmettre, diffuser, céder en crédit-bail, accorder sous licence, vendre, louer, prêter, transporter, télécharger vers une autre adresse, ni, de toute autre manière, à transférer, céder ou rendre publiquement disponible son compte d’utilisateur, le service, une partie du service, ou le matériel qu’il renferme, d’une manière quelconque. L’utilisateur n’est pas autorisé à adapter, traduire, analyser par rétro-ingénierie, décompiler ou désassembler le service, ni à essayer d’en découvrir le code source, les idées, algorithmes, méthodes, techniques, formats de fichier ou interfaces de programmation sous-jacents, ni à créer des œuvres dérivées du service ou d’une partie du service, sauf dans la mesure autorisée par le droit applicable. L’utilisateur n’est pas autorisé à supprimer, modifier, masquer, cacher, désactiver ou modifier aucun avis de droits d’auteur, de marque déposée ou d’autres droits de propriété intellectuelle, ni aucune marque, étiquette ou autre élément d’identification figurant dans le service, ni à falsifier ou effacer aucune mention d’auteur, aucune mention légale ou autre indication de l’origine du matériel, ni à présenter de manière inexacte l’origine de la propriété du service.
     La licence de l’utilisateur expire immédiatement s’il tente de contourner les mesures techniques de protection utilisées en liaison avec le service ou si, d’une manière générale, l’utilisateur utilise le service en violation des présentes conditions d’utilisation.
     L’utilisateur déclare que les informations qu’il fournit ou a fournies en liaison avec l’inscription au service et l’utilisation du service sont exactes. L’utilisation du service nécessite un nom d’utilisateur et un mot de passe personnel ou toute autre méthode d’identification de l’utilisateur approuvée par le prestataire de services (ci-après dénommés ” identifiants de l’utilisateur ”). Sauf convention contraire, les identifiants de l’utilisateur nécessitent une adresse électronique valide de l’utilisateur pour la génération du nom d’utilisateur.
     L’utilisateur doit tenir secret le mot de passe requis pour l’utilisation du service et ne le communiquer à personne. L’utilisateur ne peut pas céder ou transférer ses identifiants à un tiers et ne peut pas permettre à un tiers d’utiliser le service avec ses identifiants. Si un tiers a obtenu le mot de passe de l’utilisateur ou que l’utilisateur a un motif de penser qu’un tiers a obtenu son mot de passe, l’utilisateur doit en informer sans délai le prestataire de services. L’utilisateur est seul responsable des opérations réalisées à l’aide de son compte d’utilisateur jusqu’à ce qu’il ait informé le prestataire de services de la perte du mot de passe et que le prestataire de services ait disposé d’un délai raisonnable pour bloquer l’utilisation du service avec les identifiants de l’utilisateur.
     Le prestataire de services se réserve le droit de mettre fin à l’accès de l’utilisateur au service sans préavis si l’utilisateur viole les présentes conditions d’utilisation. Le prestataire de services est en droit de bloquer l’accès au service s’il a un motif de penser que l’utilisateur se livre à une activité illégale ou porte atteinte à la sécurité des données ou à la vie privée d’autres utilisateurs ou du prestataire de services ou si le prestataire de services est avisé que le mot de passe de l’utilisateur a été découvert par un tiers.
     Aucun appareil, ni connexion nécessaires à l’utilisation du service ne sont fournis dans le cadre des présentes conditions d’utilisation. Il incombe à l’utilisateur d’acheter, d’entretenir et de mettre à jour de tels appareils et connexions (y compris la sécurité des données) et de supporter les frais correspondants."]

    [:h2 "Propriété et droits de propriété intellectuelle"]

    [:p "Le prestataire de services détient tous les droits, titres et intérêts sur le service, ainsi que sur tout le matériel fourni à travers le service, notamment tous les droits d’auteur, brevets, marques, droits des dessins et modèles, secrets de fabrication et tous les autres droits de propriété intellectuelle (ci-après dénommés ” les droits de propriété intellectuelle ”). L’utilisateur ne saurait acquérir aucun droit de propriété à travers l’utilisation du service ou par exemple à travers le téléchargement de matériel depuis ou vers le service. À moins que cela ne soit expressément autorisé par une législation impérative, le service ne saurait être copié, reproduit ou diffusé d’une manière ou sur un support quelconques, en tout ou partie, sans le consentement préalable et écrit du prestataire de services. Tous les droits non expressément accordés à l’utilisateur dans les présentes sont réservés par le prestataire de services.
     Les matériels et informations, ainsi que tous les droits de propriété intellectuelle correspondants, que l’utilisateur intègre dans le service (ci-après dénommés ” matériel de l’utilisateur ”) demeurent la propriété de l’utilisateur ou d’un tiers."]
    [:h2 "Responsabilités du prestataire de services"]
    [:p "Le prestataire de services est responsable de la mise en œuvre et de la maintenance techniques du service. Le prestataire de services peut suspendre le service lorsque nécessaire, par exemple pour des travaux d’installation, de modification ou de maintenance ou lorsque les lois ou règlements ou les autorités l’exigent ou s’il existe d’autres motifs justifiés de suspension. Le prestataire de services essaie de faire en sorte que la suspension soit aussi brève que possible. Le prestataire de services s’efforce d’informer les utilisateurs avec un préavis raisonnable des changements et des interruptions de service substantiels sur la page de connexion au service et dans le groupe de discussion Open Badge Passport. Cependant, le prestataire de services se réserve le droit de procéder à de petites mises à jour sans information préalable. Une sauvegarde du contenu du service est effectuée une fois par jour. La sauvegarde est effectuée au cas où le contenu du service devrait être restauré à la suite, par exemple, d’un problème technique. Cependant, le contenu d’un utilisateur donné n’est pas restauré s’il le supprime accidentellement. Le prestataire de services a le droit de mettre fin au service à son entière discrétion. Le prestataire de services s’efforce de notifier la cessation du service avec un préavis raisonnable." ]
    [:p "Le prestataire de services s’engage à réparer les éventuels défauts de logiciel affectant la qualité du service selon le plan de développement du produit en vigueur."]
    [:h2 "Utilisation du service et liens"]
    [:p "Il est de la responsabilité de l’utilisateur du service de s’assurer qu’il a le droit d’utiliser les fichiers qu’il télécharge vers le service et qu’il utilise dans le service. L’utilisateur s’engage à ne pas utiliser le service d’une manière, ni, plus généralement, transmettre aucun matériel enfreignant des droits de propriété intellectuelle, le droit au respect de la vie privée, le droit à la protection de la personnalité ou tous autres droits de tiers ou constituant une violation de la loi ou une atteinte aux bonnes mœurs. Le prestataire de services n’est pas responsable des éventuelles infractions commises par les utilisateurs.
     L’utilisateur utilise le service d’une manière qui ne porte pas préjudice au prestataire de services, ni aux autres utilisateurs ou aux tiers. Si le prestataire de services est avisé que l’utilisateur a transmis un matériel constituant une infraction comme indiqué ci-dessus, le prestataire de services a le droit de supprimer ledit matériel, l’environnement de l’utilisateur ou de l’organisation de l’utilisateur ou d’en bloquer l’utilisation sans préavis.
     Le service peut contenir des liens vers des sites appartenant à des tiers ou exploités par des tiers (ci-après dénommés ” sites de tiers ”). Le prestataire de services ne saurait être responsable du contenu, ni des produits ou services proposés par des tiers. En outre, le contenu figurant sur des sites de tiers peut être soumis à des conditions d’utilisation et/ou des règles de confidentialité distinctes. Le prestataire de services recommande donc à l’utilisateur d’en prendre connaissance."]
    ]
   [:div
    [:h2 "Fourniture du service"]
    [:p " Le prestataire de services se réserve tous les droits d’apporter des changements aux conditions d’utilisation et fonctionnalités du service. Les changements substantiels des conditions d’utilisation font l’objet d’un avis sur la page de connexion à Open Badge Passport. Dès lors que l’utilisateur continue d’utiliser le service après un tel avis, il est réputé accepter les changements concernés.
     Le service est fourni ” en l’état ”, sans garantie d’aucune sorte. Le prestataire de services ne garantit pas que le service fonctionnera sans interruptions ni erreurs. Le prestataire de services décline toute responsabilité quant à l’exactitude, l’exhaustivité ou la fiabilité des informations ou de tout autre matériel présenté sur le service, ainsi qu’à l’égard du contenu ou des autres fonctionnalités des produits ou services offerts ou transmis à travers le service. LE PRESTATAIRE DE SERVICES DÉCLINE PAR LES PRÉSENTES TOUTE GARANTIE EXPRESSE, IMPLICITE ET LÉGALE, NOTAMMENT, ENTRE AUTRES, TOUTE GARANTIE IMPLICITE DE QUALITÉ MARCHANDE, DE NON-CONTREFAÇON, DE QUALITÉ SATISFAISANTE OU D’ADÉQUATION À UN USAGE PARTICULIER, DANS TOUTE LA MESURE OÙ LA LOI PERMET DE DÉCLINER DE TELLES GARANTIES. LE PRESTATAIRE DE SERVICES DÉCLINE TOUTE RESPONSABILITÉ POUR LES ACTES, OMISSIONS ET COMPORTEMENTS DE TOUS TIERS EN LIAISON AVEC L’UTILISATION DU SERVICE PAR L’UTILISATEUR.
     Le prestataire de services ne saurait être tenu pour responsable des dommages directs ou indirects occasionnés par un éventuel retard, par un changement ou une perte de service, de produit ou de matériel transféré à travers le service. Le prestataire de services n’est pas responsable des dommages directs ou indirects occasionnés par les interruptions et perturbations, y compris en cas de perte, de retard ou d’altération de données dus à des défauts techniques ou à des opérations de maintenance. En outre, le prestataire de services décline toute responsabilité pour les dommages directs ou indirects occasionnés à l’utilisateur par les programmes malveillants (virus, vers, etc.) ou par un contenu incorrect figurant dans le service. Le prestataire de services n’est pas responsable des dommages occasionnés par l’utilisateur ou par un tiers.
     Le prestataire de services n’est en aucun cas responsable des dommages indirects ou imprévisibles occasionnés à l’utilisateur dans des circonstances quelconques.
     L’utilisateur assume la responsabilité et s’engage à exonérer le prestataire de services de toute responsabilité à l’égard des dommages, frais et dettes occasionnés par l’utilisateur ou par un comportement illicite de sa part, un manquement de sa part aux présentes conditions d’utilisation ou une atteinte de sa part aux droits d’un tiers à travers l’utilisation du service ou du contenu correspondant."]
    [:h2 "Droit applicable et règlement des différends"]
    [:p "Les présentes conditions d’utilisation et la relation contractuelle correspondante sont régies par le droit finlandais. En cas de différends nés des présentes conditions d’utilisation ou de la relation contractuelle correspondante, un règlement à l’amiable est recherché. Si aucun accord n’est trouvé, le différend est tranché par le tribunal d’instance de la ville d’Oulu, en Finlande, en première instance."]
    [:p "Si vous avez des questions concernant les présentes conditions d’utilisation, veuillez contacter le prestataire de services " [:a {:href "mailto:contact@openbadgepassport.com"} "(contact@openbadgepassport.com)."]]
    [:p "La déclaration de protection des données complète les conditions d’utilisation et y est intégrée par référence."]
    [:div {:style {:padding "15px"}}
     [:h1 {:style {:text-align "center"}} "Déclaration de protection des données"]
     [:div
      [:p {:style {:text-align "center"}} "Mis à jour le 29.1.2019 "]
      [:p " "]
      [:ol {:style {:list-style-type "none"}}
       [:li [:h3 "Responsable du traitement"]
        [:div
         [:p "Open Badge Factory Oy" ]
         [:p ""]
         [:p "Kiviharjunlenkki 1 E, 90220 Oulu"]
         [:p "FINLAND"]
         [:p [:a {:href "mailto:contact@openbadgefactory.com"} "contact@openbadgefactory.com"]]
         [:p "(ci-après dénommé ” nous ” ou ” Open Badge Factory ”)"]]
        ]
       [:li [:h3 "Interlocuteur pour les questions liées au registre "]
        [:div
         [:p "Eric Rouselle" ]
         [:p "Kiviharjunlenkki 1 E, 90220 Oulu"]
         [:p "Phone number: +358 400 587 373"]
         [:p [:a {:href "mailto:dataprotection@openbadgefactory.com"} "dataprotection@openbadgefactory.com"]]]]
       [:li  [:h3 "Nom du registre"]
        [:div
         [:p "REGISTRE DES CLIENTS ET DE PROSPECTION DU SERVICE OPEN BADGE PASSPORT" ]]]
       [:li [:h3 "Quelles sont la base juridique et la finalité du traitement des données à caractère personnel?"]
        [:div
         [:p "La base du traitement de données à caractère personnel est l’intérêt légitime de Open Badge Factory reposant sur une relation avec le client ou la mise en œuvre d’un contrat avec la personne concernée, ainsi que l’obtention d’un consentement à la prospection." ]
         [:p "La base du traitement de données à caractère personnel est la suivante : "]
         [:ul
          [:li "fournir et développer notre service Open Badge Passport (ci-après dénommé le ” service ”),"]
          [:li "satisfaire à nos engagements et obligations contractuels et autres, "]
          [:li "veiller à la qualité de la relation avec le client, "]
          [:li "analyser et profiler le comportement des personnes concernées, "]
          [:li "prospection électronique et directe, "]
          [:li "publicité ciblée dans nos services en ligne ou les services en ligne d’autres entreprises."]]]]
       [:li [:h3 "Quelles données traitons-nous ?"]
        [:div
         [:p "Nous traitons les données à caractère personnel suivantes vous concernant : "]
         [:ul
          [:li [:b "informations de base sur la personne concernée, "] "telles que ses nom*, pays*, langue*, nom d’utilisateur*, mot de passe*;" ]
          [:li [:b "coordonnées de la personne concernée, "] "adresse électronique*, numéro de téléphone, adresse, localité, État;" ]
          [:li [:b "informations concernant la relation de service et le contrat, "] "telles que détails du ou des badges ouverts attribués à la personne concernée, détails du profil d’utilisateur, correspondance avec la personne concernée et autres références, cookies et données liés à l’utilisation des cookies; " ]
          [:li [:b "autres informations facultatives fournies par la personne concernée dans le service, "] "telles que : présentation personnelle dans le service, liens vers des comptes de réseaux sociaux (tels que Facebook, Linkedin, Twitter, Pinterest, Instagram, des blogues, etc.)." ]

          ]
         [:p "Notre relation contractuelle avec la personne concernée est subordonnée à la fourniture des données à caractère personnel marquées d’un astérisque, ainsi qu’à l’autorisation de l’utilisation de cookies dans le navigateur de l’utilisateur du service. Sans ces informations indispensables, nous ne sommes pas en mesure de fournir le service."]
         ]]
       [:li [:h3 "D’où recevons-nous les données ?" ]
        [:div
         [:p "Nous recevons les données à caractère personnel susmentionnées essentiellement de la part de personne concernée elle-même, car les données sont saisies dans le service par la personne concernée."]
         [:p "Nous recevons de notre client (en tant que responsable du traitement des données) l’adresse électronique de la personne concernée à laquelle nous envoyons le badge ouvert qui lui a été attribué. Nous opérons dans cette relation en tant que sous-traitant des données."]
         [:p "Pour les finalités décrites dans la présente déclaration de protection des données, des données à caractère personnel peuvent en outre être collectées et mises à jour à partir de sources disponibles publiquement et d’après des informations reçues d’autorités ou d’autres tiers dans les limites des lois et règlements applicables. La mise à jour des données de ce type est réalisée manuellement ou à l’aide de procédés automatisés. "]

         ]]
       [:li [:h3 "À qui communiquons-nous les données et transférons-nous des données en dehors de l’UE ou de l’EEE?"]
        [:div
         [:p "Seul le nom d’utilisateur de la personne concernée, que chaque utilisateur peut définir lui-même, s’affiche pour les autres utilisateurs du service."]
         [:p "Nous traitons les informations nous-mêmes et utilisons des sous-traitants qui traitent les données à caractère personnel pour notre compte. Nous avons sous-traité la gestion informatique à un prestataire de services externe et les données sont conservées sur son serveur. Le serveur est protégé et géré par ledit prestataire de services externe."]
         [:p "Les données peuvent être communiquées aux autorités en application de dispositions impératives. Nous ne communiquons pas d’informations du registre à des acteurs extérieurs."]
         [:p "Nous pouvons communiquer des informations agrégées et anonymes vous concernant à des fins de prospection, publicité, recherche, respect de la réglementation ou à d’autres fins."]
         [:p "Nous ne transférons pas de données à caractère personnel en dehors l’UE/EEE."]
         ]]
       [:li [:h3 "Comment protégeons-nous les données et combien de temps les conservons-nous ?"]
        [:div
         [:p "Les données à caractère personnel sont collectées dans des bases de données qui sont protégées par des pare-feu, des mots de passe et d’autres mesures techniques. Les bases de données et leurs copies de sauvegarde sont placées dans des locaux fermés et seules peuvent y accéder certaines personnes désignées d’avance, c’est-à-dire uniquement ceux de nos employés qui ont le droit de traiter les données des clients dans le cadre de leur travail. Ces personnes sont, d’une part, le personnel du service à la clientèle du prestataire de services et, d’autre part, les administrateurs techniques du service. Chaque utilisateur a un nom d’utilisateur et un mot de passe personnels pour l’accès au système."]
         [:p "La personne concernée peut à tout moment ajouter, modifier et supprimer toutes ses données du service, voire supprimer son compte entièrement. La suppression du compte efface du service toutes les données de la personne concernée."]
         [:p "Nous conservons les données autant qu’il est nécessaire pour la finalité du traitement des données. Nous évaluons régulièrement la nécessité de conservation des données en tenant compte de la législation applicable. En outre, nous prenons les mesures raisonnables en vue de nous assurer de ne pas conserver des données à caractère personnel incompatibles, périmées ou inexactes dans le registre compte tenu de la finalité du traitement."]
         ]]
       [:li [:h3 "Quels sont vos droits en tant que personne concernée ?"]
        [:div
         [:p "En tant que personne concernée, vous avez un droit d’accès aux données à caractère personnel vous concernant qui sont conservées dans le registre et le droit d’exiger la rectification ou l’effacement des données. Cela peut être fait en accédant, modifiant et/ou supprimant vos données à caractère personnel conservées dans le service après vous être connecté au service. Si vous avez besoin d’assistance, veuillez contacter la personne indiquée à la section 2 ci-dessus. " ]
         [:p "Vous avez aussi le droit de retirer ou modifier votre consentement à la prospection."]
         [:p "En tant que personne concernée, vous avez le droit de vous opposer au traitement ou de demander la limitation du traitement et d’introduire une réclamation auprès d’une autorité de contrôle compétente en matière de traitement des données à caractère personnel."]
         [:p "Pour des raisons personnelles spécifiques, vous avez aussi le droit de vous opposer au profilage et à tout autre traitement vous concernant lorsque le traitement des données repose sur la relation avec le client. Dans le cadre de votre réclamation, vous devez indiquer le cas précis dans lequel vous vous opposez au traitement. Nous pouvons refuser de prendre en compte une telle demande en application de la loi."]
         [:p "En tant que personne concernée, vous avez le droit de vous opposer au traitement à tout moment gratuitement, y compris au profilage dans la mesure où il est lié à une finalité de prospection."]
         ]]

       [:li [:h3 "Qui pouvez-vous contacter ?"]
        [:div
         [:p "Tous les contacts et toutes les demandes concernant la présente déclaration de protection des données doivent être adressés par écrit ou présentés en personne à l’interlocuteur indiqué à la section deux (2)."]
         ]]
       [:li [:h3 "Modifications de la déclaration de protection des données"]
        [:div
         [:p "Si nous apportons des changements à la présente déclaration de protection des données, nous publierons la déclaration modifiée sur notre site Web, en mentionnant la date de la modification. Si les modifications sont importantes, nous pouvons aussi vous en informer par d’autres moyens, par exemple en vous envoyant un courriel ou en insérant un avis sur notre page d’accueil. Nous vous recommandons d’examiner ces principes de protection des données de temps à autre, afin de vous assurer que vous avez eu connaissance des modifications apportées."]]]
       ]
      ]
     ]
    ]])


(defn ^:export terms []
  [:div {:style {:padding "30px"}}
   [:div
    [:h1 {:style {:text-align "center"}} "TERMS OF USE"]]
   [:div {:style {:text-align "center"}}
    [:p "Updated 29.1.2019 "]]
   [:div
    [:h2 "General information about the use of Open Badge Passport service"]
    [:p  "The Open Badge Passport service (later referred to as Service) is
     offered by Open Badge Factory Oy (later referred to as Service Provider). The Service delivered by the Service Provider consist of a cloud based platform,
     where Open Badges earners can receive, share, manage and publish their badges.  The terms of Use apply to the users
     of the Service (later referred to as User(s)). User refers to 2 user levels: 1) Users, who use the service for personal
     purposes 2) customer organization, which has it’s own environment in the Service."]
    [:br]
    [:p "By using the Open Badge Passport service the User accepts these Terms of Use."]
    ]
   [:div
    [:h2 "Access to the Service and restrictions"]
    [:p "By creating a User account in the Service,
     the User assures the Service Provider that he/she is at least 13 years old and if under 18 years,
     authorized by his/her parents. "]
    [:p "Subject to the User´s acceptance of and continuing compliance with these Terms of Use, the Service Provider grants to the User a limited, non-exclusive, non-transferable, revocable license to use the Service solely for the purpose of receiving, managing, displaying, sharing and publishing Open Badges. The User agrees not to use the Service for any other purpose. The Service shall not be used in any other manner than via specified interfaces and according to given instructions and these Terms of Use provided by the Service Provider.
     Except as specifically allowed in these Terms of Use, the User is not entitled to use, copy, reproduce, republish, store, modify, transfer, display, encode, transmit, distribute, lease, license, sell, rent, lend, convey, upload or otherwise transfer, assign or make publicly available its User account, the Service, a part thereof or the material contained therein in any way. The User is not entitled to adapt, translate, reverse engineer, decompile, disassemble or attempt to discover the source code, underlying ideas, algorithms, methods, techniques, file formats or programming interfaces of, or create derivative works from the Service or any part thereof, except to the extent permitted under the applicable law. The User is not entitled to remove, modify, hide, obscure, disable or modify any copyright, trademark or other proprietary rights notices, marks, labels or any other branding elements contained on or within the Service, falsify or delete any author attributions, legal notices or other labels of the origin or source of the material, or misrepresent the source of ownership of the Service.
     The license of the User terminates immediately if it attempts to circumvent any technical protection measures used in connection with the Service or if the User otherwise uses the Service in breach these Terms of Use.
     The User promises that the information the User provides or provided in connection with registration to, and use of the Service is true and accurate. The use of the Service requires a username and a personal password or other user identification method approved by the Service Provider (hereinafter referred to as User ID). Unless otherwise agreed, the User ID requires valid email address of the User for generating the username.
     The User must keep the password required for the use of the Service secret and not disclose it to anyone else. User may not assign or transfer its User ID to a third party and may not allow a third party use the Service with its User ID. If a third party has obtained User´s password or the User has a reason to believe that a third party has obtained its password, the User must immediately inform the Service Provider. The User is solely responsible for actions taken by using its User account until it has informed the Service Provider of the loss of the password and the Servive Provider has had a reasonable time to prevent the use of the Service with the User ID.
     The Service Provider reserves the right to terminate User´s access to the Service without prior notice if the User violates these Terms of Use. The Service Provider is entitled to prevent access to the Service if it has reason to believe that the User is engaged in illegal activity or compromise other Users' or Service Provider´s data security or privacy or if the Service Provider receives a notice that User´s password has gotten into the hands of a third party.
     No devices or connections necessary for the use of the Service are provided subject to these Terms of Use. User is responsible for purchasing, maintaining
     and updating such devices and connections (including data security) and for any costs related thereto."]

    [:h2 "Ownership and Intellectual Property Rights"]

    [:p "The Service Provider shall own all rights, title and interest in and to the Service as well as any material in or provided through the Service, including any copyright, patent, trademark, design right, trade secret and any other intellectual property rights (hereinafter referred to as Intellectual Property Rights). The User shall not receive any ownership rights by using the Service or for example by downloading material from or submitting material to the Service. Unless expressly authorized by mandatory legislation, Service may not be copied, reproduced or distributed in any manner or medium, in whole or in part, without prior written consent of the Service Provider. All rights not expressly granted to the User herein are reserved by the Service Provider.
     The materials and information and any Intellectual Property Rights related thereto which the User inserts into the
     Service (User Material) remain the property of the User or a third party."]
    [:h2 "Responsibilities of the Service Provider"]
    [:p "The Service Provider is responsible for technical implementation and maintenance of the Service. The Service Provider may suspend the Service when necessary for example for installation, amendment or maintenance work or if laws, regulations or authorities so require or if there are other justifiable reasons for suspension. The Service Provider aims to ensure that the suspension is as short as possible. The Service Provider will make an effort to inform Users a reasonable time in advance of substantial changes and breaks in service on the login page of the Service and in the Open Badge Passport discussion group. However, the Service Provider reserves the right to perform small updates without informing about it in advance. A back-up of the service content is made once a day. The back-up is made in case the service content needs to be restored due to, for example, a technical problem. However, individual User's content is not restored, if it accidentally deletes its content.
     The Service provider has the right to terminate the Service at its sole discretion.
     The Service Provider aims to notify a reasonable time in advance about the termination of the Service." ]
    [:p "The Service Provider is committed to repairing possible software faults affecting the quality of the Service according to the valid product development plan."]
    [:h2 "Use of Service and links"]
    [:p "It is the responsibility of the User of the Service to make sure that they have the right to use the files they upload and use in the Service. User agrees not to use the Service in a manner or otherwise submit any material that violate any Intellectual Property Rights, privacy, publicity or any other rights of others; or would be illegal or violate good manner. The Service Provider is not responsible for possible violations of the Users.
     User shall use the Service in a manner that does not cause harm to the Service Provider, other Users or third parties. If the Service Provider receives a notice claiming that the User has submitted afore described material, The Service Provider is entitled to remove such material, the User or User organization's environment or prevent their use without notice.
     The Service may contain links to sites, which are owned or operated by third parties (“Third Party Sites”). The Service Provider shall not be responsible for the content or for products or services offered by third parties. Further, the content on Third Party Sites may be subject to separate terms of use and/or privacy policies, the contents of which Service Provider recommends the User to review."]
    ]
   [:div
    [:h2 "Provision of the Service"]
    [:p "The Service Provider reserves all rights to changes in the Service's Terms of Use and features. Substantial changes in the Terms of Use shall be informed about on the login page of the Open Badge Passport. User´s continued use of the Service after such notice shall be deemed an acceptance of any changes.
     The Service is provided on an “as-is” basis without warranties of any kind. The Service Provider does not warrant that the Service will function without interruptions or error-free. The Service Provider shall not be liable for the correctness, exhaustiveness or reliability of the information or other material presented on the Service nor for the content or other features of the products or services offered on or conveyed through the Service. THE SERVICE PROVIDER HEREBY DISCLAIMS ANY AND ALL EXPRESS, IMPLIED, AND STATUTORY WARRANTIES, INCLUDING, WITHOUT LIMITATION, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, SATISFACTORY QUALITY OR FITNESS FOR A PARTICULAR PURPOSE, TO THE FULL EXTENT SUCH WARRANTIES MAY BE DISCLAIMED BY LAW. THE SERVICE PROVIDER DISCLAIMS ANY AND ALL LIABILITY FOR THE ACTS,
     OMISSIONS AND CONDUCT OF ANY THIRD PARTIES IN CONNECTION WITH OR RELATED TO USER´S USE OF THE SERVICE."]
    [:p "The Service Provider shall not be liable for direct or indirect damages caused by a possible delay, a change or a loss of a service, product or material transferred through the Service. The Service provider is not liable for direct or indirect damages caused by interruptions and disturbances including loss or delay of data or changes in data due to technical defects or maintenance. Further, the Service Provider disclaims any liability for direct or indirect damages caused to the User by harmful programs (virus, worms etc.) or incorrect content in the Service, the Service provider bears no liability for damages caused by the User or by a third party."]

    [:p "The Service Provider is never liable for any indirect or unforeseeable damages caused to the User at any given circumstances."]
    [:p "User shall be liable and agree to indemnify and hold the Service Provider harmless from and against all damages,
     costs, expenses and liabilities which are caused by the User or its
     unlawful behavior or infringement of these Terms of Use or its violation of any rights of a third party through use of the Service or content
     related thereto."]
    [:h2 "Applicable law and settlement of disputes"]
    [:p "These Terms of Use and the contractual relation related thereto shall be governed by the Laws of Finland. Disputes arising out of these Terms of Use or the contractual relation related thereto will be attempted to be settled amicably. If no agreement is attained, the dispute shall be settled in the District Court of the City of Oulu, Finland as the first instance."]
    [:p "If you have questions regarding these Terms of Use, please contact the Service Provider " [:a {:href "mailto:contact@openbadgepassport.com"} "(contact@openbadgepassport.com)."]]
    [:p "The Privacy Notice document complements the Terms of Use and is incorporated into these Terms of Use by reference."]
    [:div {:style {:padding "15px"}}
     [:h1 {:style {:text-align "center"}} " Privacy Notice"]
     [:div
      [:p {:style {:text-align "center"}} "Updated 29.01.2019"]
      [:p ""]
      [:ol {:style {:list-style-type "none"}}
       [:li [:h3 "Controller"]
        [:div
         [:p "Open Badge Factory Oy" ]
         [:p ""]
         [:p "Kiviharjunlenkki 1 E, 90220 Oulu"]
         [:p "FINLAND"]
         [:p [:a {:href "mailto:contact@openbadgefactory.com"} "contact@openbadgefactory.com"]]
         [:p "(hereafter ”we” or  ”Open Badge Factory”)"]]
        ]
       [:li [:h3 "Contact person for register matters "]
        [:div
         [:p "Eric Rouselle" ]
         [:p "Kiviharjunlenkki 1 E, 90220 Oulu"]
         [:p "Phone number: +358 400 587 373"]
         [:p [:a {:href "mailto:dataprotection@openbadgefactory.com"} "dataprotection@openbadgefactory.com"]]]]

       [:li  [:h3 "Name of register"]
        [:div
         [:p "CUSTOMER AND MARKETING REGISTER FOR OPEN BADGE PASSPORT SERVICE" ]]]
       [:li [:h3 "What is the legal basis for and purpose of the processing of personal data?"]
        [:div
         [:p "The basis of processing personal data is Open Badge Factory's justified interest on the basis of a customer
          relationship or implementing a contract with the data subject, as well as consent as regards direct marketing." ]
         [:p "The basis of processing personal data is: "]
         [:ul
          [:li "the delivery and development of our Open Badge Passport Service (“Service”),"]
          [:li "fulfilling our contractual and other promises and obligations, "]
          [:li "taking care of the customer relationship, "]
          [:li "analyzing and profiling behaviour of the data subject, "]
          [:li "electronic and direct marketing, "]
          [:li "targeting advertising in our and others´ online services"]]]]
       [:li [:h3 "What data do we process?"]
        [:div
         [:p "We process the following personal data of you: "]
         [:ul
          [:li [:b "Basic information of the data subject "] "such as name*, country*, language*, user name*, password*;" ]
          [:li [:b "Contact information of the data subject "] "e-mail address*, phone number, address, city, state;" ]
          [:li [:b "Information of  the service relationship and the contract "] "such as details of the open badge(s) granted to the data subject, details of the user profile,
           correspondence with the data subject and other references, cookies and data related to use of them; " ]
          [:li [:b "Other voluntary information provided by the data subject into the Service "] "such as personal introduction in the Service, links to social media accounts
           (such as Facebook, Linkedin, Twitter, Pinterest, Instagram, blogs etc.)." ]

          ]
         [:p "Committing personal data marked with a star as well as allowing the use of cookies in the service user’s browser, is a requirement for our contractual relationship with the data subject. Without this necessary information we are not able to provide the Service."]
         ]]
       [:li [:h3 "From where do we receive data?" ]
        [:div
         [:p " We receive the above mentioned personal data primarily from the data subject him/herself, as the data is entered into the Service by the data subject."]
         [:p "We receive the data subject’s e-mail address to which we send the granted Open Badge from our customer (as the data controller). We act as the data processor in this relationship."]
         [:p "For the purposes described in this privacy notice, personal data may also be collected and updated from publicly available sources and based on information received from authorities or other third parties within the limits of the
          applicable laws and regulations. Data updating of this kind is performed manually or by automated means."]

         ]]
       [:li [:h3 "To whom do we disclose data and do we transfer data outside of EU or EEA?"]
        [:div
         [:p "Only the data subject’s user name which each user can define him/herself, is displayed to other users in the Service. "]
         [:p "We process information ourselves and use subcontractors that process personal data on behalf of and for us. We have outsourced the IT-management to an external service provider, to whose server the data is stored. The server is protected and managed by the external service provider."]
         [:p "Data may be disclosed to authorities under compelling provisions. We don’t disclose information of the register to external quarters."]
         [:p "We may disclose aggregate, anonymous information about you for marketing, advertising, research, compliance, or other purposes."]
         [:p "We do not transfer personal data outside of EU/EEA."]
         ]]
       [:li [:h3 "How do we protect the data and how long do we store them? "]
        [:div
         [:p "The personal data is collected into databases that are protected by firewalls, passwords and other technical measures. The databases and the backup copies of them are in locked premises and can be accessed only by certain pre-designated persons, i.e. only those of our employees,  who on behalf of their work are entitled to process customer data. These persons include the Service Provider's customer
          service personnel and the technical administrators of the Service. Each user has a personal username and password to the system."]
         [:p "The data subject may at any time add, change and remove all his/her data from the Service as well as delete his/her account
          entirely. The deletion of the account will erase all the data subject’s data from Service."]
         [:p "We store the data as long as it is necessary for the purpose of processing the data. We estimate regularly the need for data storage taking into account the applicable legislation. In addition, we take care of such reasonable actions of which purpose is to ensure that no incompatible, outdated or inaccurate
          personal data is stored in the register taking into account the purpose of the processing."]
         ]]
       [:li [:h3 "What are your rights as a data subject?"]
        [:div
         [:p "As a data subject you have a right to inspect the personal data conserning yourself, which is stored in the register, and a right to require rectification or erasure of the data. This may be done by accessing, modifying and/or deleting your personal data stored in the Service by logging into the Service. If you need assistance, please contact the person mentioned in Section 2 above. " ]
         [:p "You also have a right to withdraw or change your consent for direct marketing."]
         [:p "As a data subject, you have a right to object processing or request restricting the processing and lodge a complaint with a supervisory authority responsible for processing personal data."]
         [:p "For specific personal reasons, you also have a right to object profiling and other processing concerning you, when processing the data is based on the customer relationship. In connection to your claim, you should identify the specific situation on which you object the processing.
          We can refuse to act on such request on the basis of the law."]
         [:p "As a data subject you have the right to object processing at any time free of charge, including profiling in so far as it relates to direct marketing."]
         ]]

       [:li [:h3 "Who can you be in contact with?"]
        [:div
         [:p "All contacts and requests concerning this privacy notice shall be submitted in writing or in person to the person mentioned in section two (2)."]
         ]]
       [:li [:h3 "Changes in the Privacy Notice"]
        [:div
         [:p "Should we make amendments to this privacy protection statement, we will place the amended statement on our website, with an indication of the amendment date. If the amendments are significant, we may also inform you about this by other means, for example by sending an email or placing a bulletin on our homepage. We recommend that you review these privacy protection principles from time to time to ensure you are aware of any amendments made."]]]
       ]
      ]
     ]
    ]])
