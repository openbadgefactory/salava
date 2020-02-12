(ns salava.extra.boat.ui.block
  (:require [salava.core.i18n :refer [t]]))

(defn ^:export footer []
  [:div.footer
   [:div.footer-container
    [:p
     "droits d'auteur © 2019 BOAT"
     #_[:a {:class "bottom-link" :href "mailto:info@badgbl.nl"}
        ""]]
    [:p
     "Powered by Open Badge Factory ®. Open Badge Factory ® is a registered trademark"]]])

(defn ^:export terms []
  [:div.terms-container
   [:div ;{:style {:padding "15px"}}
    [:h1.text-center.blue-text "CONDITIONS GÉNÉRALES D’UTILISATION"]
    [:h2.sr-only "CONDITIONS GÉNÉRALES D’UTILISATION"]
    [:div.text-center
     [:p [:b "Mis à jour le 10 février 2020"]]]
    [:div
     [:h2 [:b "Informations générales sur l’utilisation du service b-connexion"]]
     [:p
      "Le service " [:span.blue-text "b-connexion"] " (ci-après dénommé ” le service ”) est proposé par" [:b " l’Université Confédérale Léonard de Vinci "] "(aussi dénommée “le prestataire de services”),
        dans le cadre du réseau" [:b " “B.O.A.T (Badges Ouverts à Tous) - Reconnaître en Nouvelle Aquitaine”."]]
     [:p "Le service fourni par l’Université Confédérale Léonard de Vinci consiste en une" [:b " plate-forme hébergée sur un serveur en France qui permet aux bénéficiaires de badges ouverts "]  "(“open badges”)"
      [:b " de recevoir, partager, gérer et publier leurs badges. "]]
     [:p "Ces conditions d’utilisation s’appliquent aux utilisateurs du service (ci-après dénommés ”l’utilisateur ” ou ” les utilisateurs ”). Lors de la création et/ou de la gestion de l’espace personnel de l’utilisateur, l’utilisateur est amené à remplir des formulaires et communiquer des données à caractère personnel afin qu’il puisse accéder à et bénéficier de l’ensemble du service proposé."]
     [:p "En créant un compte dans " [:span.blue-text "b-connexion"] ", l'utilisateur accepte les conditions d'utilisation présentées dans ce document."]]
    [:div
     [:h2 [:b "1. Accès au service et limitations"]]
     [:p "En créant un compte d’utilisateur dans le service, l’utilisateur déclare au prestataire de services qu’il est âgé d’au moins 15 ans."]
     [:h3 [:b "1.1 Utilisateurs de moins de 15 ans"]]
     [:p "La loi « Informatique et Libertés » impose le recueil du consentement conjoint de l’enfant de moiet du titulaire de l’autorité parentale. Le service b-connexion n’est pas actuellement en mesure de recueillir directement ce consentement."]
     [:h3 [:b "1.2 Utilisateurs de plus de 15 ans"]]
     [:p "Sous réserve que l’utilisateur accepte et respecte les présentes conditions générales d’utilisation, le prestataire de services accorde à l’utilisateur une licence limitée, non exclusive, incessible et révocable d’utilisation du service dans le seul but de recevoir, gérer, afficher, partager et publier des badges ouverts. "]
     [:p "L’utilisateur s’engage à n’utiliser le service dans aucun autre but. Le service ne saurait être utilisé d’aucune autre manière qu’à l’aide des interfaces spécifiées et dans le respect des instructions données et des présentes conditions générales d’utilisation communiquées par le prestataire de services."]
     [:p "Aucun appareil, ni connexion nécessaires à l’utilisation du service ne sont fournis dans le cadre des présentes conditions générales d’utilisation. Il incombe à l’utilisateur d’acheter, d’entretenir et de mettre à jour de tels appareils et connexions (y compris la sécurité de ses données) et de prendre en charge les frais correspondants."]
     [:p "L’utilisateur déclare que les informations qu’il fournit ou a fournies en liaison avec l’inscription au service et l’utilisation du service sont exactes. L’utilisation du service nécessite un nom d’utilisateur et un mot de passe personnel ou toute autre méthode d’identification approuvée par le prestataire de services (ci-après dénommés ” identifiants de l’utilisateur ”). L’inscription de l’utilisateur nécessite qu’il ait une adresse électronique valide et puisse la vérifier pour que soit généré un nom d’utilisateur."]
     [:p "L’utilisateur doit tenir secret le mot de passe requis pour l’utilisation du service et ne le communiquer à personne. L’utilisateur ne peut pas céder ou transférer ses identifiants à un tiers et ne peut pas permettre à un tiers d’utiliser le service avec ses identifiants. "]
     [:h3 [:b "1.3 Responsabilités du prestataire de services"]]
     [:p "Le prestataire de services est responsable de la mise en œuvre et de la maintenance techniques du service. Le prestataire de services peut suspendre le service lorsque nécessaire, par exemple pour des travaux d’installation, de modification ou de maintenance ou lorsque les lois ou règlements ou les autorités l’exigent ou s’il existe d’autres motifs justifiés de suspension. Le prestataire de services essaie de faire en sorte que la suspension soit aussi brève que possible. Le prestataire de services s’efforce d’informer les utilisateurs avec un préavis raisonnable des changements et des interruptions de service substantiels sur la page de connexion au service."]
     [:p "Cependant, le prestataire de services se réserve le droit de procéder à de petites mises à jour sans information préalable. Une sauvegarde du contenu du service est effectuée une fois par jour. La sauvegarde est effectuée au cas où le contenu du service devrait être restauré à la suite, par exemple, d’un problème technique. Cependant, le contenu d’un utilisateur donné n’est pas restauré s’il le supprime accidentellement. Le prestataire de services a le droit de mettre fin au service à  son entière discrétion. "]
     [:p "Le prestataire de services s’engage à faire les démarches nécessaires auprès d’OPEN BADGE FACTORY pour que d’éventuels défauts de logiciel affectant la qualité du service soit corrigés, selon le plan de développement du produit en vigueur."]
     [:h3 [:b "1.4 Fourniture du service"]]
     [:p "Le prestataire de services se réserve tous les droits d’apporter des changements aux conditions d’utilisation et fonctionnalités du service. Les changements substantiels des conditions d’utilisation font l’objet d’un avis sur la page de connexion à b-connexion. Dès lors que l’utilisateur continue d’utiliser le service après un tel avis, il est réputé accepter les changements concernés."]
     [:p "Le service est fourni “en l’état ”, sans garantie d’aucune sorte. Le prestataire de services ne garantit pas que le service fonctionnera sans interruptions ni erreurs. Le prestataire de services décline toute responsabilité quant à l’exactitude, l’exhaustivité ou la fiabilité des informations ou de tout autre matériel présenté sur le service, ainsi qu’à l’égard du contenu ou des autres fonctionnalités des produits ou services offerts ou transmis à travers le service."]
     [:p "LE PRESTATAIRE DE SERVICES DÉCLINE PAR LES PRÉSENTES TOUTE GARANTIE EXPRESSE, IMPLICITE ET LÉGALE, NOTAMMENT, ENTRE AUTRES, TOUTE GARANTIE IMPLICITE DE QUALITÉ MARCHANDE, DE NON-CONTREFAÇON, DE QUALITÉ SATISFAISANTE OU D’ADÉQUATION À UN USAGE PARTICULIER, DANS TOUTE LA MESURE OÙ LA LOI PERMET DE DÉCLINER DE TELLES GARANTIES. LE PRESTATAIRE DE SERVICES DÉCLINE TOUTE RESPONSABILITÉ POUR LES ACTES, OMISSIONS ET COMPORTEMENTS DE TOUS TIERS EN LIAISON AVEC L’UTILISATION DU SERVICE PAR L’UTILISATEUR. "]
     [:p "Le prestataire de services ne saurait être tenu pour responsable des dommages directs ou indirects occasionnés par un éventuel retard, par un changement ou une perte de service, de produit ou de matériel transféré à travers le service. Le prestataire de services n’est pas responsable des dommages directs ou indirects occasionnés par les interruptions et perturbations, y compris en cas de perte, de retard ou d’altération de données dus à des défauts techniques ou à des opérations de maintenance. "]
     [:p "En outre, le prestataire de services décline toute responsabilité pour les dommages directs ou indirects occasionnés à l’utilisateur par les programmes malveillants (virus, vers, etc.) ou par un contenu incorrect figurant dans le service. Le prestataire de services n’est pas responsable des dommages occasionnés par l’utilisateur ou par un tiers. Le prestataire de services n’est en aucun cas responsable des dommages indirects ou imprévisibles occasionnés à l’utilisateur dans des circonstances quelconques. L’utilisateur assume la responsabilité et s’engage à exonérer le prestataire de services de toute responsabilité à l’égard des dommages, frais et dettes occasionnés par l’utilisateur ou par un comportement illicite de sa part, un manquement de sa part aux présentes conditions d’utilisation ou une atteinte de sa part aux droits d’un tiers à travers l’utilisation du service ou du contenu correspondant."]
     [:h3 [:b "1.5 Propriété et droits de propriété intellectuelle"]]
     [:p "En dehors des cas spécifiques autorisés dans les présentes conditions générales d’utilisation, l’utilisateur n’est pas autorisé à utiliser, copier, reproduire, re-publier, stocker, modifier, transférer, afficher, encoder, transmettre, diffuser, céder en crédit-bail, accorder sous licence, vendre, louer, prêter, transporter, télécharger vers une autre adresse, ni, de toute autre manière, à transférer, céder ou rendre publiquement disponible son compte d’utilisateur, le service, une partie du service, ou le matériel qu’il renferme."]
     [:p "L’utilisateur n’est pas autorisé à adapter, traduire, analyser par rétro-ingénierie, décompiler ou désassembler le service, ni à essayer d’en découvrir le code source, les idées, algorithmes, méthodes, techniques, formats de fichier ou interfaces de programmation sous-jacents, ni à créer des œuvres dérivées du service ou d’une partie du service, sauf dans la mesure autorisée par le droit applicable."]
     [:p "L’utilisateur n’est pas autorisé à supprimer, modifier, masquer, cacher, désactiver ou modifier un avis de droits d’auteur, de marque déposée ou d’autres droits de propriété intellectuelle, ni aucune marque, étiquette ou autre élément d’identification figurant dans le service, ni à falsifier ou effacer aucune mention d’auteur, aucune mention légale ou autre indication de l’origine du matériel, ni à présenter de manière inexacte l’origine de la propriété du service."]
     [:p "Le prestataire de services détient tous les droits, titres et intérêts sur le service, ainsi que sur tout le matériel fourni à travers le service, notamment tous les droits d’auteur, brevets, marques, droits des dessins et modèles, secrets de fabrication et tous les autres droits de propriété intellectuelle (ci-après dénommés ” les droits de propriété intellectuelle ”). "]
     [:p "L’utilisateur ne saurait acquérir le droit de propriété à travers l’utilisation du service ou par exemple à travers le téléchargement de matériel depuis ou vers le service. À moins que cela ne soit expressément autorisé par une législation impérative, le service ne saurait être copié, reproduit ou diffusé d’une manière ou sur un support quelconques, en tout ou partie, sans le consentement préalable et écrit du prestataire de services. "]
     [:p "Tous les droits non expressément accordés à l’utilisateur dans les présentes conditions générales d’utilisation sont réservés par le prestataire de services. Les matériels et informations, ainsi que tous les droits de propriété intellectuelle correspondants, que l’utilisateur intègre dans le service (ci-après dénommés ” matériel de l’utilisateur ”) demeurent la propriété de l’utilisateur ou d’un tiers."]
     [:p "La licence de l’utilisateur expire immédiatement s’il tente de contourner les mesures techniques de protection utilisées en liaison avec le service ou si, d’une manière générale, l’utilisateur utilise le service en violation des présentes conditions générales d’utilisation."]
     [:h3 [:b "1.6 Droits d’accès, de suppression, d’opposition "]]
     [:p "Il est de la responsabilité de l’utilisateur du service de s’assurer qu’il a le droit d’utiliser les fichiers qu’il télécharge vers le service et qu’il utilise dans le service. L’utilisateur s’engage à ne pas enfreindre les droits de propriété intellectuelle, le droit au respect de la vie privée, le droit à la protection de la personnalité ou tous autres droits de tiers ou constituant une violation de la loi ou une atteinte aux bonnes mœurs dans la manière dont il utilise le service ou en transmettant du contenu."]
     [:p "Le prestataire de services n’est pas responsable des éventuelles infractions commises par les utilisateurs. L’utilisateur utilise le service d’une manière qui ne porte pas préjudice au prestataire de services, ni aux autres utilisateurs ou aux tiers. Si le prestataire de services est avisé que l’utilisateur a transmis un contenu constituant une infraction comme indiqué ci-dessus, le prestataire de services a le droit de supprimer ledit contenu, l’environnement de l’utilisateur ou de l’organisation de l’utilisateur ou d’en bloquer l’utilisation sans préavis."]
     [:p "Le service peut contenir des liens vers des sites appartenant à des tiers ou exploités par des tiers (ci-après dénommés ” sites de tiers ”). Le prestataire de services ne saurait être responsable du contenu, ni des produits ou services proposés par des tiers. En outre, le contenu figurant sur des sites de tiers peut être soumis à des conditions d’utilisation et/ou des règles de confidentialité distinctes. Le prestataire de services recommande donc à l’utilisateur d’en prendre connaissance, notamment en ce qui concern les réseaux sociaux."]
     [:p "L’utilisateur peut supprimer son compte directement dans les paramètres du service (Utilisateur / supprimer le compte) ; il peut également avoir accès à l’ensemble des données qui sont utilisées par le service (Utilisateur / Mes données) ; enfin, en dehors de l’affichage de son pseudonyme, il peut gérer ou supprimer les données spécifiques qu’il affiche (géolocalisation, “À propos de moi…”). La suppression du compte efface du service toutes les données de l’utilisateur concerné."]
     [:p "Si un tiers a obtenu le mot de passe de l’utilisateur ou que l’utilisateur a un motif de penser qu’un tiers a obtenu son mot de passe, l’utilisateur doit en informer sans délai le prestataire de services. L’utilisateur est seul responsable des opérations réalisées à l’aide de son compte l’utilisateur jusqu’à ce qu’il ait informé le prestataire de services de la perte du mot de passe et que le prestataire de services ait disposé d’un délai raisonnable pour bloquer l’utilisation du service avec les identifiants de l’utilisateur."]
     [:p "Le prestataire de services se réserve le droit de mettre fin à l’accès de l’utilisateur au service sans préavis si l’utilisateur viole les présentes conditions générales d’utilisation. Le prestataire de services est en droit de bloquer l’accès au service s’il a un motif de penser que l’utilisateur se livre à une activité illégale ou porte atteinte à la sécurité des données ou à la vie privée d’autres utilisateurs ou du prestataire de services ou si le prestataire de services est avisé que le mot de passe de l’utilisateur a été découvert par un tiers."]
     [:h3 [:b "1.7 Droit applicable et règlement des différends"]]
     [:p "Conformément au Règlement (UE) 2016/679 du Parlement européen et du Conseil du 27 avril 2016 relatif à la protection des personnes physiques à l'égard du traitement des données à caractère personnel et à la libre circulation de ces données et à la loi n° 78-17 du 6 janvier 1978 relative à l'informatique, aux fichiers et aux libertés, les utilisateurs de b-connexion disposent des droits suivants qu’ils peuvent exercer à tout moment en prenant contact auprès du prestataire de services"     [:a {:href "mailto:contact@u-ldevinci.fr"} " (contact@u-ldevinci.fr):"]]
     [:ul
      [:li "droit d’accès, de rectification et d’opposition au traitement de ses données ; "]
      [:li "droit à la limitation du traitement de ses données ; "]
      [:li "droit à la portabilité de ses données ;"]
      [:li "droit à l’effacement et à l’oubli numérique."]]
     [:p "ou par courrier postal à :"]
     [:ol {:style {:list-style-type "none"}}
      [:li "Université Confédérale Léonard de Vinci"]
      [:li "M. le Délégué général"]
      [:li "2 avenue Gustave Eiffel"]
      [:li "BP 80 184"]
      [:li "86962 CHASSENEUIL FUTUROSCOPE CEDEX"]]
     [:p "En cas de différends nés des présentes conditions d’utilisation ou de la relation contractuelle correspondante, un règlement à l’amiable est recherché. Si aucun accord n’est trouvé, le différend est tranché par le tribunal administratif de Poitiers, dans le ressort duquel siège l’« Université confédérale Léonard de Vinci »."]
     [:div
      [:h2 [:b "2. Les données traitées par b-connexion"]]
      [:h3 [:b "2.1 Responsable de traitement"]]
      [:p "Les données à caractère personnel sont collectées et traitées par :"]
      [:p "L’Université Confédérale Léonard de Vinci (UCLdV), établissement public à caractère administratif régi par le décret n° 2015-857 du 13 juillet 2015 portant approbation des statuts de la communauté d'universités et établissements « Université confédérale Léonard de Vinci», situé 2 avenue Gustave Eiffel, bâtiment H6, 1er étage, 86962 CHASSENEUIL FUTUROSCOPE CEDEX, n° SIRET 130 021 280 000 24."]
      [:p "L’UCLdV peut être contactée en adressant un courrier postal à l’adresse suivante :"]
      [:ol {:style {:list-style-type "none"}}
       [:li "Université Confédérale Léonard de Vinci"]
       [:li "M. le Délégué général"]
       [:li "2 avenue Gustave Eiffel"]
       [:li "BP 80 184"]
       [:li "86962 CHASSENEUIL FUTUROSCOPE CEDEX"]
       [:li "Ou via le mail suivant: " [:a {:href "mailto:contact@u-ldevinci.fr"} "contact@u-ldevinci.fr"]]]
      [:h3 [:b "2.2 Les données à caractère personnel collectées et traitées"]]
      [:p "“Toute information se rapportant à une personne physique identifiée ou identifiable, directement ou indirectement, notamment par référence à un identifiant, tel qu'un nom, un numéro d'identification, des données de localisation, un identifiant en ligne, ou à un ou plusieurs éléments spécifiques propres à son identité physique, physiologique, génétique, psychique, économique, culturelle ou sociale” est comprise dans la notion de “donnée à caractère personnel” (Article 4 du règlement général sur la protection des données.)"]
      [:p "Sur " [:span.blue-text "b-connexion"] ", lorsque l’utilisateur crée un compte, seul son nom d’utilisateur (qu’il définit lui-même) s’affiche pour les autres utilisateurs du service. Il peut ensuite choisir de compléter son profil et de rendre visibles d’autres informations."]
      [:p "Lors de l’utilisation des différentes fonctionnalités du service, des données sont collectées et traitées :"]
      [:ol {:style {:list-style-type "none"}}
       [:li "-  données d’identification comprenant nom, prénom, adresse électronique, mot de passe, langue, pays ;"]
       [:li "-  informations facultatives fournies par l’utilisateur : adresses de courriel supplémentaires ; photo ou avatar ; présentation personnelle ;"]
       [:li "-  informations concernant la relation de service et le contrat :  détails du ou des badges ouverts attribués à la personne concernée ;"]
       [:l1 "-  Liens vers des comptes de réseaux sociaux extérieurs au service: Facebook, Linkedin, Twitter, Google ;"]
       [:li "-  données de localisation pour situer ses badges sur une carte propre au service à partir des informations de géolocalisation fournies par l’utilisateur."]]
      [:p "Ces informations sont saisies par l’utilisateur lors de la création de son compte ou de l’utilisation du service. Le prestataire de service collecte également des informations de navigation comprenant l’historique des consultations, adresses IP, paramètres de navigateur."]
      [:p "Les données à caractère personnel suivantes sont nécessaires pour le fonctionnement du service " [:span.blue-text "b-connexion"] " : adresse électronique, mot de passe."]
      [:h3 [:b "2.3 Les finalités des traitements"]]
      [:p "Le prestataire de service collecte ou traite les données personnelles des utilisateurs dans le respect des lois sur la protection des données européennes et françaises :"]
      [:ul
       [:li "pour accéder à et fournir le service auquel s’est inscrit l’utilisateur ou, à sa demande, pour supprimer son compte sur le service ;"]
       [:li "pour traiter et répondre à une demande, à un téléchaB2 Network s’interdit toute connexion sur tout serveur en utilisant les protocoles TELNET, RSH ou RLOGIN, ou en utilisant SSH en version 1 du protocole. Pour ses besoins d’exécution de services sur les serveurs du Client, B2 Network s’interdit l’utilisation du protocole FTP pour le transfert de fichier.rgement, ou à toute autre transaction ;"]
       [:li "pour détecter des problèmes techniques ou de service ;"]
       [:li "pour améliorer les activités et services du prestataire de service ;"]
       [:li "pour identifier les tendances d’utilisation et pratiquer l’analyse de données en vue de déterminer l’efficacité et évaluer la performance du service, sous réserve d’avoir obtenu le consentement explicite et préalable de l’utilisateur ;"]
       [:li "pour se conformer aux obligations légales, prévenir ou détecter des fraudes, abus, utilisations illicites, violations des Conditions Générales d’Utilisation, et pour se conformer à des décisions de justice et requêtes gouvernementales."]]
      [:p "Les données personnelles communiquées par l’utilisateur sont utilisées pour détecter des problèmes techniques ou de service et en informer OPEN BADGE FACTORY, qui assure les mises à jour et le développement du service, mais elles ne sont pas transmises à OPEN BADGE FACTORY."]]
     [:div
      [:h2 [:b "3. Consentement"]]
      [:p "L’utilisateur dispose à tout moment du droit de retirer son consentement donné, de s’opposer à tout traitement de ses données personnelles collectées et de supprimer son compte sur " [:span.blue-text "b-connexion"] "."]
      [:p "Le prestataire de service s’engage à recueillir le consentement explicite, spécifique et préalable de l’utilisateur, et/ou à permettre à l’utilisateur de s’opposer à l’utilisation de ses données à caractère personnel pour certaines finalités, et/ou à accéder et/ou à rectifier des informations le concernant."]]
     [:div
      [:h2 [:b "4. Cookies"]]
      [:p "À la suite de l’inscription de l’utilisateur sur " [:span.blue-text "b-connexion"] ", un seul cookie est utilisé à chaque nouvelle connexion de l’utilisateur : salava-passport-session. Ce cookie permet de reconnaître l’utilisateur, de le garder connecté pendant la durée de la session, et de vérifier à chaque nouvelle session que son compte n'a pas été bloqué depuis sa dernière connexion."]
      [:p "Le service ne peut pas être utilisé sans ce cookie."]]
     [:div
      [:h2 [:b "5. Transmission des données à caractère personnel à des tiers"]]
      [:p "Les données à caractère personnel collectées par le prestataire sont susceptibles d’être transmises à des destinataires externes. Cette transmission se fera dans le cadre de conventions signées avec l’Université Confédérale Léonard de Vinci, exclusivement pour les besoins des finalités indiquées au point 2.3. Préalablement aux transferts, l’Université Confédérale Léonard de Vinci prendra toutes les mesures et garanties nécessaires pour sécuriser de tels transferts."]
      [:p "Des transferts peuvent ainsi être réalisés dans le cadre des activités et services suivants : gestion emailing, recherche et statistiques."]
      [:p "Le prestataire de service ne transmet aucune donnée à caractère personnel à des partenaires à des fins d’opérations commerciales ou à toutes autres fins qui ne répondent pas aux besoins des finalités indiquées au point 2.3."]
      [:p "Le prestataire de service ne transmet aucune donnée à caractère personnel en dehors de l’Union européenne."]
      [:p "Dans l’hypothèse où le prestataire de service envisagerait de modifier sa politique de transfert de données en vue d’effectuer des transferts à des partenaires notamment commerciaux, ceci pourra se faire exclusivement sous réserve d’avoir obtenu le consentement explicite et préalable de l’Utilisateur."]]
     [:div
      [:h2 [:b "6. Conservation des données"]]
      [:p "Les données à caractère personnel sont conservées pour la durée nécessaire à la réalisation des finalités pour lesquelles elles sont traitées et selon les recommandations de la CNIL au regard de la norme simplifiée n° NS-048 relative aux traitements automatisés de données à caractère personnel relatifs à la gestion de clients et de prospects."]
      [:p "Au-delà de cette durée, les données associées à un identifiant sont supprimées."]
      [:table.table.table-bordered
       [:tbody
        [:tr
         [:td "Finalité du traitement"]
         [:td "Licéité - Base juridique"]
         [:td "Durée de conservation en base active"]
         [:td "Archivage"]]
        [:tr
         [:td "Créer un compte sur le site"]
         [:td "Contrat"]
         [:td "2 ans si inactivité de l’usager depuis sa dernière connexion, et non consentement suite à sollicitation du prestataire de service"]
         [:td "NA"]]]]]
     [:div
      [:h2 [:b "7. Mesures de sécurité techniques et organisationnelles mises en œuvre pour l’activité de traitement."]]
      [:p "Les données à caractère personnel sont collectées dans des bases de données qui sont protégées par des pare-feu, des mots de passe et d’autres mesures techniques. Les bases de données et leurs copies de sauvegarde sont placées dans des locaux fermés et seules peuvent y accéder certaines personnes désignées d’avance, c’est-à-dire uniquement ceux de nos employés qui ont le droit de traiter les données des clients dans le cadre de leur travail. Ces personnes sont, d’une part, le personnel du service à la clientèle du prestataire de services et, d’autre part, les administrateurs techniques du service. Chaque utilisateur a un nom d’utilisateur et un mot de passe personnels pour l’accès au système."]
      [:p "Les données à caractère personnel collectées et traitées sont hébergées par B2 Network, B2 Network SARL, 15 rue Jean Claret 63000 Clermont Ferrand - France dans le centre de données Equinix PA3, à Paris Saint Denis - France."]
      [:p "L’accès au serveur hébergé par B2 Network est effectué sur seules instructions du client, et dans le cadre de l’exécution des Services fourni par B2 Network pour le Client. Les accès dont bénéficie B2 Network sont protégés, dans le cas d’un serveur Linux ou Unix, par une authentification à base de clef SSH protégée par un mot de passe connu seulement de l’opérateur."]
      [:p "B2 Network s’interdit toute connexion sur tout serveur en utilisant les protocoles TELNET, RSH ou RLOGIN, ou en utilisant SSH en version 1 du protocole. Pour ses besoins d’exécution de services sur les serveurs du Client, B2 Network s’interdit l’utilisation du protocole FTP pour le transfert de fichier."]]]]])
