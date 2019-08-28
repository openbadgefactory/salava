(ns salava.extra.hpass.ui.block
  (:require [salava.core.i18n :refer [t]]))

(defn ^:export login_top []
  [:div.login-top-container {:key "login_top"}
   [:div.login_top
    [:h1
     "Welcome to myHPass"]
    [:hr]]]
  )

(defn ^:export login_bottom []
  [:div.login-bottom {:key "login_buttom"}
   [:br]
   [:p [:b "Get Recognised. Build your professional profile. Advance your career."][:br]
    "myHPass is a free platform for you as a humanitarian or volunteer. Store your digital badges and share your skills, learning and experience. "]
   [:br]
   [:a {:href "http://hpass.org/"} "Learn more about HPass"]
   [:br]
   [:br]
   [:div.footer]
   ])

(defn ^:export footer []
  [:footer.footer
   [:div.footer-container
    [:p.text-muted
     "Copyright © 2019 Humanitarian Leadership Academy | "
     [:a {:class "bottom-link" :href "mailto:info@hpass.org" }
      "info@hpass.org"]
     " | "
     [:a {:class "bottom-link" :href "https://hpass.org/privacy-policy-2/" :target "_blank" }
      "Privacy Policy and Terms of Use"]

     ]]])

(defn ^:export terms []
  [:div {:style {:padding "30px"}}
   [:div
    [:h1 {:style {:text-align "center"}} [:b "PRIVACY POLICY"]]
    [:p "This Privacy Policy covers the information practices of the HPass platform (my.hpass.org) and the website which is used to access the HPass platform (hpass.org) (Site). The data controller of this website is the Humanitarian Leadership Academy (Academy), a charity registered in England and Wales (1161600) and a company limited by guarantee in England and Wales (9395495). The Academy’s registered office is at 1 St John’s Lane, London, EC1M 4AR"]
    [:p "The Academy believes strongly in protecting your privacy and the confidentiality of your Personal Information. We acknowledge that you may have privacy and security concerns with respect to the information we collect, use, and disclose to third parties for the purpose of allowing us to offer and provide our products and services to you. In order to comply with privacy legislation, we have developed this Privacy Policy. Personal information is any combination of information, in the possession of or likely to come into the possession of the Academy, that can be used to identify, contact, or locate a discrete individual (“Personal Information”) and will be treated in accordance with this Privacy Policy. This includes any expression of opinion about such individual. Any information which cannot be used to identify a discrete individual (such as aggregated statistical information) is not Personal Information."]]
   [:div
    [:h2 [:b "PURPOSE FOR COLLECTION, USE AND DISCLOSURE OF PERSONAL INFORMATION"]]
    [:p "The Academy processes your Personal Information for the legitimate interest of fulfilling the mission and vision of the Academy as a humanitarian, not-for-profit organization. In furtherance of this legitimate interest, the Academy collects, uses and discloses your Personal Information (including individuals associated with you, all of whom are referred to as “you” in this Privacy Policy) in its normal course of operating the Site for the following purposes:"]
    [:ul
     [:li [:p "Establishing and maintaining our communications with you;"]]
     [:li [:p "Publishing humanitarian credentials;"]]
     [:li [:p "Development and delivery of learning and credentialing services;"]]
     [:li [:p "Responding to your inquiries about badging opportunities, humanitarian aid opportunities, and other award and learning services;"]]
     [:li [:p "Enabling your communications with other users of the Site; including learning providers, badge providers, aid organizations, and other individuals with similar interests;"]]
     [:li [:p "Connecting you with others who share the same interests and commitment to humanitarian aid; Meeting legal, security, and regulatory requirements;"]]
     [:li [:p "Protecting against fraud, suspicious or other illegal activities; and"]]
     [:li [:p "Compiling statistics for analysis of our sites and our business."]]]]
   [:div
    [:h2 [:b "WHAT INFORMATION WE COLLECT"]]
    [:p "The information gathered by the Academy from this Site falls into two categories: (1) information voluntarily supplied by visitors to our Site and (2) tracking information gathered as visitors navigate through our Site."]
    [:div
     [:h3 [:b "INFORMATION VOLUNTARILY PROVIDED BY YOU"]]
     [:p "When using this Site, you may choose to provide us with information to help us serve your needs. The Personal Information that we collect will depend on how you choose to use this Site."]
     [:ul
      [:li [:p "Where you request information:"]
       [:p "If you request a brochure or further information from us, we require you to submit your name, e-mail address, the name of your organization, and the country in which you are based so we may send you the material you have requested, and to enable us to identify if you have an existing relationship with the Academy."]]
      [:li
       [:p "Where you register with us and/or request services:"]
       [:p "If you register with the Site, or request a service available on the Site (via another organization or a learning/badging provider), we may ask you for your name, e-mail address, country, telephone number and the reason for your communication, as well as information about your position and organization and such other information as is reasonably necessary so that we can facilitate the service. On the data submission form, we shall indicate by way of an asterisk, which information is optional and which information is mandatory. This information can include:"]
       [:ol {:style {:list-style-type "circle"}}
        [:li [:p "Information you provide on applications or other forms, which may include your name, address, email address, age, medical and health information, employment and educational information, and any awards or credentials you may possess;"]]
        [:li [:p "Information we acquire from and/or transfer to other persons (such as learning providers, industry associations, credentialing providers, humanitarian aid organizations, and your employer) to verify your identity and the accuracy of the information you have provided;"]]
        [:li [:p "Information about you received from the Academy affiliates, other intermediaries, third party providers and others which are necessary for the provision of the HPass platform; and"]]
        [:li [:p "Information we receive from background verification companies."]]]
       [:p "Any Personal Information that you send us will be used for the purpose indicated on the Site or in this Privacy Policy.Following an inquiry into receiving services from us, if you decide to proceed, we will collect Personal Information necessary to proceed with a transaction, such as your name, address, post code, contact telephone number, e-mail address, and other Personal Information as relevant to the service or credential. We will use your Personal Information to administer your account, coordinate services, etc. and generally manage your relationship with us. We shall pass your personal data to other users of the Site (including humanitarian aid agencies, learning service providers, and credentialing providers) as is necessary to enable the services and activities to which you have indicated an interest. Please see the Disclosure of Your Information section below for information on the categories of recipients of your personal data."]
       ]
      [:li [:p "Where you submit content:"]
       [:p "Finally, when you submit content regarding any of the information that you view on our Site, we will ask for your name and e-mail address, so that, if you choose, we can update you by e-mail when others also comment on the content, and also so that we can manage the content in line with our HPass Platform Terms and Conditions. You should be aware that the information you provide to the social network features of the Site will be made publically available to the Academy employees and other users of the Site. "]]]]
    [:div
     [:h3 [:b "WEBSITE NAVIGATIONAL INFORMATION"]]
     [:p "As you navigate the Site, we may also collect information through the use of commonly-used information-gathering tools, such as cookies and web beacons (collectively “Website Navigational Information”). Website Navigational Information includes standard information from your web browser (such as browser type and browser language), your Internet Protocol (“IP”) address, and the actions you take on the Site (such as the web pages viewed and the links clicked)."]
     [:p {:style {:font-size "20px"}} "Cookies"]
     [:p "Like many companies, we may use cookies on this Site. Cookies are pieces of information shared between your web browser and a website. Use of cookies enables a faster and easier experience for the user. A cookie cannot read data off your computer’s hard drive."]
     [:p "There are different kinds of cookies with different functions:"]
     [:ul {:style {:list-style-type "lower-roman"}}
      [:li [:p "Session cookies: these are only stored on your computer during your web session. They are automatically deleted when the browser is closed. They usually store an anonymous session ID allowing you to browse a website without having to log in to each page. They do not collect any information from your computer."]]
      [:li [:p "Persistent cookies: a persistent cookie is one stored as a file on your computer, and it remains there when you close your web browser. The cookie can be read by the website that created it when you visit that website again."]]
      [:li [:p "First-party cookies: the function of this type of cookie is to retain your preferences for a particular website for the entity that owns that website. They are stored and sent between the Academy’s servers and your computer’s hard drive. They are not used for anything other than for personalization as set by you. These cookies may be either Session or Persistent cookies."]]
      [:li [:p "Third-party cookies: the function of this type of cookie is to retain your interaction with a particular website for an entity that does not own that website. They are stored and sent between the Third-party’s server and your computer’s hard drive. These cookies are usually Persistent cookies."]]
      ]
     [:p "Except as described in this Privacy Policy, we do not use third-party cookies on our Sites, although we do use third party provided web beacons (please see the section on Web Beacons below)."]
     [:p "This Site does use Google Analytics, a web analytics service provided by Google, Inc. (“Google”). Google Analytics uses cookies to help the website analyze how users use the site. The information generated by the cookie about your use of the website (including your IP address) will be transmitted to and stored by Google on servers in the United States. Google will use this information for the purpose of evaluating your use of the website, compiling reports on website activity, and providing other services relating to website activity and internet usage for the Academy and its affiliates. Google may also transfer this information to third parties where required to do so by law, or where such third parties process the information on Google’s behalf. Google will not associate your IP address with any other data held by Google. You may refuse the use of cookies by selecting the appropriate settings on your browser, however please note that if you do this you may not be able to use the full functionality of this website. By using this website,
      you consent to the processing of data about you by Google in the manner and for the purposes set out above."]
     [:p "The major browsers have attempted to implement the draft “Do Not Track” (“DNT”) standard of the World Wide Web Consortium (“W3C”) in their latest releases. As this standard has not been finalized, the Academy’s Sites are not compatible with DNT."]
     [:p "For information on all of these categories of cookies, and for more information generally on cookies please refer to" [:a {:href "https://www.aboutcookies.org/"} " aboutcookies.org."]]
     [:p "We use cookies for the following purposes:"]
     [:ul
      [:li [:p [:b "Where strictly necessary"]]
       [:p "These cookies are essential in order to enable you to move around the Site and use its features, such as accessing secure areas of the Site. Without these cookies, services you have asked for, such as obtaining a quote or logging into your account, cannot be provided. These cookies do not gather information about you that could be used for marketing or remembering where you have been on the internet."]]
      [:li [:p [:b "Performance"]]
       [:p "These cookies collect information about how visitors use a Site, for instance which pages visitors go to most often, and if they get error messages from web pages. They also allow us to record and count the number of visitors to the Site, all of which enables us to see how visitors use the Site in order to improve the way that our Site works. These cookies do not collect information that identifies a person, as all information these cookies collect is anonymous and is used to improve how our Site works."]]
      [:li [:p [:b "Functionality"]]
       [:p "These cookies allow our Site to remember choices you make (such as your user name, language or the region you are in) and provide enhanced features. For instance, a Site may be able to remember your log in details, so that you do not have to repeatedly sign in to your account when using a particular device to access our Site. These cookies can also be used to remember changes you have made to text size, font and other parts of web pages that you can customize. They may also be used to provide services you have requested such as viewing a video or commenting on an article. The information these cookies collect is usually anonymized. They do not gather any information about you that could be used for advertising or remember where you have been on the internet."]]
      ]
     [:p "Please consult your web browser’s ‘Help’ documentation or visit" [:a {:href "https://www.aboutcookies.org/"} " aboutcookies.org"] " for more information about how to turn cookies on and off for your browser."]
     [:p {:style {:font-size "20px"}}  "Web Beacons"]
     [:p "The Site may also use web beacons (including web beacons supplied or provided by third parties) alone or in conjunction with cookies to compile information about users’ usage of the Site and interaction with e-mails from the Academy. Web beacons are clear electronic images that can recognize certain types of information on your computer, such as cookies, when you viewed a particular Site tied to the web beacon, and a description of a Site tied to the web beacon. We use web beacons to operate and improve the Sites and e-mail communications. We may use information from web beacons in combination with other data we have about our clients to provide you with information about the Academy and our services. We will conduct this review on an anonymous basis."]
     [:p {:style {:font-size "20px"}} "IP Addresses"]
     [:p "When you visit our Sites, the Academy collects your Internet Protocol (“IP”) addresses to track and aggregate non-Personal Information. For example, the Academy uses IP addresses to monitor the regions from which users navigate the Sites. IP addresses will be stored in such a way so that you cannot be identified from the IP address."]

     ]
    [:div
     [:h2 [:b "DISCLOSURE OF INFORMATION TO OTHERS"]]
     [:p "We do not disclose any Personal Information about you to any third parties except as stated in this Privacy Policy, as otherwise permitted by law, or authorized by you."]
     [:p "Note that as HPass has social networking features, and is intended to publish your capabilities for the purpose of helping many different parties coordinate efficient and effective humanitarian aid, most participants on the HPass platform will have your Personal Information disclosed to them as a feature of HPass."]
     [:p "Third parties to whom we disclose information are required by law and contractual undertakings to keep your Personal Information confidential and secure, and to use and disclose it for purposes that a reasonable person would consider appropriate in the circumstances, in compliance with all applicable legislation, which purposes are as follows:"]
     [:ul
      [:li [:p "To provide the functionality of the Site;"]]
      [:li [:p "To notify you or allow other users of the Site to notify you of products, services, or aid opportunities offered by other users of the Site or our affiliated companies;"]]
      [:li [:p "To update information with credentialing and learning providers;"]]
      [:li [:p "To allow you to communicate with other users in the community of interest supported by HPass; and"]]
      [:li [:p "To process transactions through data processing service providers."]]]
     [:p "If these third parties wish to use your Personal Information for any other purpose, they will have a legal obligation to notify you of this and, where required, to obtain your consent. Contact us at " [:a {:href "mailto:info@hpass.org"} "info@hpass.org"] " for more information on these third parties."]
     [:h3 [:b "AFFILIATE SHARING"]]
     [:p "In the normal course of maintaining HPass, Personal Information may be shared within the Academy and its affiliates for all the purposes above, as well as: research and statistical purposes, system administration, and crime prevention or detection. When you supply us with information containing third party Personal Information (names, addresses, or other information relating to living individuals), we will hold and use that Personal Information to provide the functionality of the HPass platform for you on the understanding that the individuals to whom the Personal Information relates have been informed of the reason(s) for obtaining the Personal Information, the fact that it may be disclosed to third parties such as the Academy, and have consented to such disclosure and use."]
     [:h3 [:b "SERVICE PROVIDERS"]]
     [:p "Because a number of the service providers we use are located in the United States, including certain the Academy affiliates, your Personal Information may be processed and stored inside the United States, and the U.S. government, courts, or law enforcement or regulatory agencies may be able to obtain disclosure of your Personal Information under US laws."]
     [:p "The Academy’s service suppliers adhere to the same protections regarding the collection, use, and retention of data as we do."]
     [:h3 [:b "BUSINESS TRANSACTIONS"]]
     [:p "As we continue to develop our mission, we might sell or buy assets. In such transactions, user information, including Personal Information, generally is one of the transferred assets. Also, if either the Academy itself or substantially all of the Academy assets were acquired, your Personal Information may be one of the transferred assets. Therefore, we may disclose and/or transfer your Personal Information to a third party in these circumstances"]
     [:h3 [:b "OTHER LEGALLY REQUIRED DISCLOSURES"]]
     [:p "The Academy preserves the right to disclose without your prior permission any Personal Information about you or your use of this Site if the Academy has a good faith belief that such action is necessary to: (a) protect and defend the rights, property or safety of the Academy, employees, other users of this Site, or the public; (b) enforce the terms and conditions that apply to use of this Site; (c) as required by a legally valid request from a competent governmental authority; or (d) respond to claims that any content violates the rights of third-parties. We may also disclose Personal Information as we deem necessary to satisfy any applicable law, regulation, legal process, or governmental request."]
     [:p "In this Privacy Policy, the purposes identified above and in “Collection, Use and Disclosure of Personal Information” will be referred to as the “Identified Purposes.”"]
     ]
    [:div
     [:h2 [:b "CALIFORNIA’S “SHINE THE LIGHT” LAW"]]
     [:p "California Civil Code Section 1798.83 requires any operator of a website to permit its California-resident customers to request and obtain from the operator a list of what Personal Information the operator disclosed to third parties for direct marketing purposes, for the preceding calendar year; and the addresses and names of such third parties. the Academy does not share any Personal Information collected from this site with third parties for their direct marketing purposes."]
     ]
    [:div
     [:h2 [:b "CONSENT"]]
     [:p "Your knowledge of and consent to the Academy’s collection, use and disclosure of your Personal Information is critical. We rely on the following actions by you as indications of your consent to our existing and future Personal Information practices:"]
     [:ul
      [:li [:p "Your voluntary provision of Personal Information to us directly;"]]
      [:li [:p "Your express consent or acknowledgement contained within a written, verbal or electronic application or claims process; and"]]
      [:li [:p "Your verbal consent solicited by the Academy (or our agent) for a specified purpose."]]
      ]
     [:p "Where the Academy relies on consent for the fair and lawful processing of Personal Information, the opportunity to consent will be provided when the Personal Information in question is collected. Your consent may be given through your authorized representative such as a legal guardian, agent or holder of a power of attorney."]
     [:p "Subject to certain legal or contractual restrictions and reasonable notice, you may withdraw this consent at any time. the Academy will inform you of the consequences of withdrawing your consent."
      [:b " In some cases, refusing to provide certain Personal Information or withdrawing consent"] " for the Academy to collect, use or disclose your Personal Information"
      [:b " could mean that we cannot obtain insurance coverage or other requested products, services or information for you."]]
     [:p [:b "If you wish to withdraw your consent please refer to the Questions or to Withdraw Consent section below."]]
     [:p [:b "However"] ", there are a number of instances where the Academy does not require your consent to engage in the processing or disclosure of Personal Information. The Academy may not solicit your consent for the processing or transfer of Personal information for the those purposes which have a statutory basis, such as:"]
     [:ul
      [:li [:p "The transfer or processing is necessary for the performance of a contract between you and the Academy (or one of its affiliates);"]]
      [:li [:p "The transfer or processing is necessary for the performance of a contract, concluded in your interest, between the Academy (or one of its affiliates) and a third party;"]]
      [:li [:p "The transfer or processing is necessary, or legally required, on important public interest grounds, for the establishment, exercise, or defense of legal claims, or to protect your vital interests; or"]]
      [:li [:p "The transfer or processing is required, or permitted without consent, by applicable law."]]
      ]
     ]
    [:div
     [:h2 [:b "LIMITING COLLECTION AND RETENTION OF PERSONAL INFORMATION"]]
     [:p "The Academy will collect, use, or disclose Personal Information that is necessary for the Identified Purposes or as permitted by law. If we require Personal Information for any other purpose, you will be notified of the new purpose, and subject to your consent (where appropriate), that new purpose will become an Identified Purpose."]
     [:p "The Academy will collect Personal Information by fair and lawful means. We will normally retain Personal Information as long as necessary for the fulfillment of the Identified Purposes. However, some Personal Information may be retained for longer periods as required by law, contract, or auditing requirements."]

     ]
    [:div
     [:h2 [:b "SAFEGUARDS"]]
     [:p "We have in place physical, electronic and procedural safeguards appropriate to the sensitivity of the information we maintain. Safeguards will vary depending on the sensitivity, format, location, amount, distribution and storage of the Personal Information. They include physical, technical, and managerial measures to keep Personal Information protected from unauthorized access. Among such safeguards are the encryption of communications via SSL, encryption of information while it is in storage, firewalls, access controls, separation of duties, and similar security protocols. However, due to the nature of the Internet and related technology, we cannot absolutely guarantee the security of Personal Information, and the Academy expressly disclaims any such obligation."]
     ]
    [:div
     [:h2 [:b "EXTERNAL LINKS"]]
     [:p "The Academy Sites may include links to other websites whose privacy policies we do not control. Once you leave our servers (you can tell where you are by checking the URL in the location bar on your web browser), use of any Personal Information you provide is governed by the privacy policy of the operator of the website you are visiting. That policy may differ from ours. If you can’t find the privacy policy of any of these websites via a link from the website’s homepage, you should contact the website directly for more information."]
     ]
    [:div
     [:h2 [:b "ACCURACY, ACCOUNTABILITY, OPENNESS AND CUSTOMER ACCESS"]]
     [:p "Our knowing about changes to some of your Personal Information (e.g. email address) may be key to effectively communicating with you at your request.
      If any of your details change you can update us by e-mailing us at "[:a {:href "mailto:info@hpass.org"} "info@hpass.org."]
      [:b " Please keep us informed of changes to your Personal Information."]]
     [:p "You have the right to access your Personal Information and request rectification of any Personal Information in the file that may be obsolete, incomplete or incorrect.
      If you have any questions about this Privacy Policy or want to access your Personal Information, you can obtain our Personal Information Question/Request form by writing or calling our Chief Privacy Officer at the following address or visiting the
      Site at: " [:a {:href "https://www.humanitarianleadershipacademy.org/privacy-cookie-policy/"} "www.humanitarianleadershipacademy.org/privacy-cookie-policy."]]
     [:div
      [:p "The Company Secretary"]
      [:p "Humanitarian Leadership Academy"]
      [:p "1 St John’s Lane"]
      [:p "London"]
      [:p "EC1M 4AR."]
      ]
     [:p "or email at" [:a {:href "mailto:compliance@humanitarian.academy"} " compliance@humanitarian.academy"]]
     ]
    [:p "The Academy is responsible for all Personal Information under its control and has designated a privacy officer who is accountable to Management for the Academy’s compliance with this Privacy Policy."]
    [:div
     [:h2 [:b "QUESTIONS OR TO WITHDRAW CONSENT"]]
     [:p "You may exercise your right to withdraw your consent to applicable uses or disclosures of your Personal Information (which may limit or terminate the products and services that the Academy provides to you) by writing or sending an email to us at the above address. We will need to validate the identity of anyone making such a request to ensure that we do not provide your information to anyone who does not have the right to such information."]
     [:p "Normally we will respond to access requests within 30 days."]
     [:p "Should you feel that your Personal Information has not been handled in accordance with this Policy after inquiring with us, you may contact the UK’s Information Commissioner’s Office with your complaint."]
     ]
    [:div
     [:h2 [:b "CHANGES TO THIS PRIVACY POLICY"]]
     [:p "If there is any material change to your rights under this Privacy Policy, the Academy will provide you with notice of such change 30 days prior to the changes going into effect. As part of this notice, the Academy may post a notice of the change on the site in a clear and conspicuous manner for the 30 day notice period. the Academy may also communicate the change via email or postal mail if this is the way that March normally corresponds with you. Please note that your continued use of the Site or Services once this 30 day period are over indicates your agreement to the changes which were the subject of the notice."]
     ]
    ]])

(defn ^:export accept-terms-string []
  "I have read and i agree to the privacy policy")

(defn ^:export welcomeblockbody [lang]
  (case lang
    "en" "Welcome to HPass! We enable humanitarians to demonstrate their skills and experience using digital badges. You can earn badges on a range of humanitarian themes, from a wide range of organisations. Set up a profile or browse available badges to get started"
    "Welcome to HPass! We enable humanitarians to demonstrate their skills and experience using digital badges. You can earn badges on a range of humanitarian themes, from a wide range of organisations. Set up a profile or browse available badges to get started."
    )
  )

(defn ^:export importbadgetext [lang]
  [:div.import-button-desc {:key "import"}
   [:i (case lang
         "en" "Import badges from other platforms, and add them to your myHPass profile"
         "Import badges from other platforms, and add them to your myHPass profile"
         )]])
