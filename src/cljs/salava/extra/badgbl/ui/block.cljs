(ns salava.extra.badgbl.ui.block
  (:require [salava.core.i18n :refer [t]]))

(defn ^:export footer []
  [:div.footer ;:footer.footer
   [:div.footer-container
    [:p.text-muted
     "Auteursrecht © 2019 Open Badge Fabriek | "
     [:a {:class "bottom-link" :href "mailto:info@badgbl.nl"}
      "info@badgbl.nl"]]
    [:p.text-muted
     "Powered by Open Badge Factory ®. Open Badge Factory ® is a registered trademark"]]])

(defn ^:export terms []
  [:div.terms-container #_{:style {:padding "15px"}}
   [:h1 {:style {:text-align "center"}} "Privacybeleid"]
   [:h2 {:style {:display "none"}} "Privacybeleid"]
   [:div
    [:p {:style {:text-align "center"}} "Bijgewerkt 05.09.2019"]
    [:p ""]
    [:ol {:style {:list-style-type "none"}}
     [:li [:h3 "Controller"]
      [:div
       [:p "Open Badge Fabriek"]
       [:p ""]
       [:p "Nelson Mandelaboulevard 22 34, 5342 CZ Oss"]
       [:p "Nederland"]
       [:p [:a {:href "mailto:info@openbadgefabriek.nl"} "info@openbadgefabriek.nl"]]
       [:p "(hierna ”wij” of ”Open Badge Fabriek”)"]]]

     [:li [:h3 "Contactpersoon voor registratiezaken "]
      [:div
       [:p "Martijn Bos"]
       [:p "Nelson Mandelaboulevard 22 34, 5342 CZ Oss"]
       [:p "Nederland"]
       [:p "Telefoonnummer: 06 20301222"]
       [:p [:a {:href "mailto:info@openbadgefabriek.nl"} "info@openbadgefabriek.nl"]]]]

     [:li  [:h3 "Naam van het register"]
      [:div
       [:p "KLANTEN- EN MARKETINGREGISTER VOOR OPEN BADGE PASPOORT SERVICE"]]]
     [:li [:h3 "Wat is de wettelijke basis voor en het doel van de verwerking van persoonsgegevens?"]
      [:div
       [:p "De basis voor de verwerking van persoonsgegevens is het gerechtvaardigde belang van Open Badge Factory op basis van een klantrelatie of de uitvoering van een overeenkomst met de betrokkene, alsmede de toestemming voor direct marketing."]
       [:p "De basis voor de verwerking van persoonlijke gegevens is:"]
       [:ul
        [:li "de levering en ontwikkeling van onze Open Badge Passport Service (”Service”),"]
        [:li "het nakomen van onze contractuele en andere beloften en verplichtingen, "]
        [:li "het verzorgen van de klantrelatie, "]
        [:li "het analyseren en profileren van het gedrag van de betrokkene, "]
        [:li "elektronische en direct marketing, "]
        [:li "gericht op reclame in onze en andermans online diensten"]]]]
     [:li [:h3 "Welke gegevens verwerken we?"]
      [:div
       [:p "Wij verwerken de volgende persoonlijke gegevens van u: "]
       [:ul
        [:li "Basisgegevens van de betrokkene zoals naam*, land*, taal*, taal*, gebruikersnaam*, wachtwoord*"]
        [:li "Contactgegevens van het e-mailadres van de betrokkene*, telefoonnummer, adres, woonplaats, stad, staat"]
        [:li "Informatie over de servicerelatie en het contract, zoals details van de open badge(s) die aan de betrokkene zijn toegekend, details van het gebruikersprofiel, correspondentie met de betrokkene en andere referenties, cookies en gegevens met betrekking tot het gebruik ervan; "]
        [:li "Andere vrijwillige informatie die door de betrokkene in de Dienst wordt verstrekt, zoals persoonlijke introductie in de Dienst, links naar sociale media-accounts (zoals Facebook, Linkedin, Twitter, Pinterest, Instagram, blogs etc.)."]]

       [:p "Het vastleggen van persoonlijke gegevens gemarkeerd met een sterretje en het toestaan van het gebruik van cookies in de browser van de gebruiker van de dienst, is een vereiste voor onze contractuele relatie met de betrokkene. Zonder deze noodzakelijke informatie zijn wij niet in staat om de Dienst te leveren."]]]

     [:li [:h3 "Waarvan ontvangen wij de gegevens?"]
      [:div
       [:p " Wij ontvangen de bovengenoemde persoonlijke gegevens voornamelijk van de betrokkene zelf, omdat de gegevens door de betrokkene in de Dienst worden ingevoerd."]
       [:p "Wij ontvangen het e-mailadres van de betrokkene waarnaar wij de toegekende Open Badge van onze klant (als verantwoordelijke voor de verwerking van de gegevens) sturen. In deze relatie treden wij op als verantwoordelijke voor de verwerking van de gegevens."]
       [:p "Voor de in deze privacyverklaring beschreven doeleinden kunnen persoonlijke gegevens ook uit openbare bronnen en op basis van informatie van overheden of andere derden binnen de grenzen van de geldende wet- en regelgeving worden verzameld en geactualiseerd. Het bijwerken van deze gegevens wordt handmatig of automatisch uitgevoerd."]]] [:li [:h3 "Aan wie geven we gegevens vrij en dragen we gegevens over buiten de EU of de EER?"]
                                                                                                                                                                                                                                                                                                                                                              [:div
                                                                                                                                                                                                                                                                                                                                                               [:p "Alleen de gebruikersnaam van de betrokkene, die elke gebruiker zelf kan definiëren, wordt aan andere gebruikers in de Dienst getoond "]
                                                                                                                                                                                                                                                                                                                                                               [:p "Wij verwerken de informatie zelf en maken gebruik van onderaannemers die voor en namens ons persoonsgegevens verwerken. Wij hebben het IT-beheer uitbesteed aan een externe dienstverlener, aan wiens server de gegevens worden opgeslagen. De server wordt beschermd en beheerd door de externe dienstverlener."]
                                                                                                                                                                                                                                                                                                                                                               [:p "Gegevens kunnen onder dwingende bepalingen aan autoriteiten worden verstrekt. Wij geven geen informatie van het register door aan externe partijen."]
                                                                                                                                                                                                                                                                                                                                                               [:p "Wij kunnen geaggregeerde, anonieme informatie over u vrijgeven voor marketing, reclame, onderzoek, naleving of andere doeleinden."]
                                                                                                                                                                                                                                                                                                                                                               [:p "Wij dragen geen persoonlijke gegevens over buiten de EU/EER."]]]

     [:li [:h3 "Hoe beschermen we de gegevens en hoe lang bewaren we ze? "]
      [:div
       [:p "De persoonlijke gegevens worden verzameld in databases die beschermd worden door firewalls, wachtwoorden en andere technische maatregelen. De databanken en de back-up kopieën ervan bevinden zich in een afgesloten ruimte en zijn alleen toegankelijk voor bepaalde vooraf aangewezen personen, d.w.z. alleen die van onze medewerkers, die ten behoeve van hun werk het recht hebben om klantgegevens te verwerken. Tot deze personen behoren het personeel van de klantenservice van de Service Provider en de technische beheerders van de Service. Elke gebruiker heeft een persoonlijke gebruikersnaam en wachtwoord voor het systeem."]
       [:p "De betrokkene kan te allen tijde al zijn/haar gegevens toevoegen, wijzigen en verwijderen uit de Service, alsmede zijn/haar account volledig verwijderen. Het verwijderen van de account wist alle gegevens van de betrokkene uit de Dienst."]
       [:p "Wij bewaren de gegevens zolang als nodig is voor de verwerking van de gegevens. Wij schatten regelmatig de behoefte aan gegevensopslag in, rekening houdend met de geldende wetgeving. Bovendien zorgen wij ervoor dat er geen incompatibele, verouderde of onnauwkeurige persoonlijke gegevens in het register worden opgeslagen, rekening houdend met het doel van de verwerking."]]]

     [:li [:h3 "Wat zijn uw rechten als betrokkene?"]
      [:div
       [:p "Als betrokkene heeft u recht op inzage in de persoonsgegevens die op u betrekking hebben en die in het register zijn opgeslagen, en het recht om correctie of verwijdering van de gegevens te eisen. Dit kan worden gedaan door uw persoonlijke gegevens die zijn opgeslagen in de Dienst te openen, te wijzigen en/of te verwijderen door in te loggen op de Dienst. Als u hulp nodig hebt, neem dan contact op met de persoon die in punt 2 hierboven wordt genoemd. "]
       [:p "U heeft ook het recht om uw toestemming voor direct marketing in te trekken of te wijzigen."]
       [:p "Als betrokkene heeft u het recht om bezwaar te maken tegen de verwerking of om een beperking van de verwerking te verzoeken en een klacht in te dienen bij een toezichthoudende instantie die verantwoordelijk is voor de verwerking van persoonsgegevens."]
       [:p "Om specifieke persoonlijke redenen heeft u ook het recht om bezwaar te maken tegen het opstellen van profielen en andere verwerkingen die op u betrekking hebben, wanneer de verwerking van de gegevens gebaseerd is op de klantenrelatie. In verband met uw claim dient u de specifieke situatie te identificeren waarin u bezwaar maakt tegen de verwerking. Wij kunnen een dergelijk verzoek op grond van de wet weigeren."]
       [:p "Als betrokkene heeft u te allen tijde het recht om kosteloos bezwaar te maken tegen de verwerking, inclusief het opstellen van profielen, voor zover deze betrekking hebben op direct marketing."]]] [:li [:h3 "Met wie kunt u in contact komen?"]
                                                                                                                                                                                                                  [:div
                                                                                                                                                                                                                   [:p "Alle contacten en verzoeken met betrekking tot deze privacyverklaring dienen schriftelijk of persoonlijk te worden ingediend bij de in lid 2 (2) genoemde persoon."]]]

     [:li [:h3 "Wijzigingen in de Privacy Verklaring"]
      [:div
       [:p "Mochten wij wijzigingen aanbrengen in deze privacyverklaring, dan zullen wij de gewijzigde verklaring op onze website plaatsen, met vermelding van de wijzigingsdatum. Als de wijzigingen belangrijk zijn, kunnen wij u hier ook op andere manieren over informeren, bijvoorbeeld door het sturen van een e-mail of het plaatsen van een bulletin op onze homepage. Wij raden u aan deze principes voor de bescherming van de privacy van tijd tot tijd door te nemen om er zeker van te zijn dat u op de hoogte bent van eventuele wijzigingen."]]]]]])
