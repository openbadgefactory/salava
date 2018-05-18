(ns salava.core.ui.terms
  (:require [salava.core.i18n :refer [t]]))

(defn default-terms-fr []
  [:div {:style {:padding "30px"}}
   [:div
    [:h1 {:style {:text-align "center"}} "TERMS OF USE"]]
   [:div
    [:h2 "General information about the use of Open Badge Passport service"]
    [:p  "The Open Badge Passport service (later referred to as Service) is
     offered by Discendum Oy (later referred to as Service Provider). The Service delivered by the Service Provider consist of a cloud based platform,
     where Open Badges earners can receive, share, manage and publish their badges.  The terms of Use apply to the users
     of the Service (later referred to as User(s)). User refers to 2 user levels: 1) Users, who use the service for personal
     purposes 2) customer organization, which has it’s own environment in the Service."]]])

(defn default-terms []
  [:div {:style {:padding "30px"}}
   [:div
    [:h1 {:style {:text-align "center"}} "TERMS OF USE"]]
   [:div
    [:h2 "General information about the use of Open Badge Passport service"]
    [:p  "The Open Badge Passport service (later referred to as Service) is
     offered by Discendum Oy (later referred to as Service Provider). The Service delivered by the Service Provider consist of a cloud based platform,
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
    [:p "If you have questions regarding these Terms of Use, please contact the Service Provider. (contact@openbadgepassport.com)"]
    [:p "The Privacy Notice document complements the Terms of Use and is incorporated into these Terms of Use by reference."]
    [:div {:style {:padding "15px"}}
   [:h1 {:style {:text-align "center"}} " Privacy Notice"]
   [:div
    [:ol
     [:li [:h3 "Controller"]
      [:div
       [:p "Discendum Oy" ]
       [:p ""]
       [:p "Kiviharjunlenkki 1 E, 90220 Oulu"]
       [:p "Phone number: 020 718 1850 "]
       [:p "info@discendum.com"]
       [:p "(hereafter ”we” or  ”Discendum”)"]]
      ]
     [:li [:h3 "Contact person for register matters "]
      [:div
       [:p "Esko Pulkkinen" ]
       [:p "Kiviharjunlenkki 1 E, 90220 Oulu"]
       [:p "Phone number: 020 718 1850"]
       [:p "dataprotection@openbadgefactory.com"]]]

     [:li  [:h3 "Name of register"]
      [:div
       [:p "CUSTOMER AND MARKETING REGISTER FOR OPEN BADGE PASSPORT SERVICE" ]]]
      [:li [:h3 "What is the legal basis for and purpose of the processing of personal data?"]
      [:div
       [:p "The basis of processing personal data is Discendum’s justified interest on the basis of a customer
        relationship or implementing a contract with the data subject, as well as consent as regards direct marketing." ]
       [:p "The basis of processing personal data is: "]
       [:ul
        [:li "the delivery and development of our Open Badge Passeport Service (“Service”),"]
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

#_(defn privacy-terms []
  [:div {:style {:padding "15px"}}
   [:h1 {:style {:text-align "center"}} " Privacy Notice"]
   [:div
    [:ol
     [:li [:h3 "Controller"]
      [:div
       [:p "Discendum Oy" ]
       [:p ""]
       [:p "Kiviharjunlenkki 1 E, 90220 Oulu"]
       [:p "Phone number: 020 718 1850 "]
       [:p "info@discendum.com"]
       [:p "(hereafter ”we” or  ”Discendum”)"]]
      ]
     [:li [:h3 "Contact person for register matters "]
      [:div
       [:p "Esko Pulkkinen" ]
       [:p "Kiviharjunlenkki 1 E, 90220 Oulu"]
       [:p "Phone number: 020 718 1850"]
       [:p "dataprotection@openbadgefactory.com"]]]

     [:li  [:h3 "Name of register"]
      [:div
       [:p "CUSTOMER AND MARKETING REGISTER FOR OPEN BADGE PASSPORT SERVICE" ]]]
      [:li [:h3 "What is the legal basis for and purpose of the processing of personal data?"]
      [:div
       [:p "The basis of processing personal data is Discendum’s justified interest on the basis of a customer
        relationship or implementing a contract with the data subject, as well as consent as regards direct marketing." ]
       [:p "The basis of processing personal data is: "]
       [:ul
        [:li "the delivery and development of our Open Badge Passeport Service (“Service”),"]
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
  )
