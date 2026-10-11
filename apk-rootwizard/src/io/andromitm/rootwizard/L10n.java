package io.andromitm.rootwizard;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * AndroMITM RootWizard — i18n FR/EN (choix selon la langue de l'appareil).
 * Textes longs volontairement en code : un seul point de maintenance.
 */
public final class L10n {

    private static final Map<String, String> M = new HashMap<>();

    static {
        if (Locale.getDefault().getLanguage().startsWith("fr")) fr(); else en();
    }

    private L10n() { }

    public static String s(String k) {
        String v = M.get(k);
        return v != null ? v : k;
    }

    private static void p(String k, String v) { M.put(k, v); }

    // ════════════════════════════════ FR ════════════════════════════════
    private static void fr() {
        p("app_title", "AndroMITM RootWizard");
        p("app_sub", "Assistant de root guidé — Blackview Xplore1 · Redmi Note 14 · Android 14/15");

        p("p_title", "Profil de l'appareil");
        p("p_auto", "Auto");
        p("p_std", "Blackview / AOSP");
        p("p_xiaomi", "Xiaomi / Redmi");
        p("btn_miunlock", "Ouvrir la page Mi Unlock (officiel)");
        p("btn_fw_xiaomi", "Firmwares Xiaomi/HyperOS (archives)");

        p("warn_title", "À lire avant tout");
        p("warn_body",
                "• Le déverrouillage du bootloader EFFACE TOUTES LES DONNÉES du téléphone.\n"
                + "• La garantie peut être annulée.\n"
                + "• Les apps bancaires et Google Wallet peuvent cesser de fonctionner (Play Integrity).\n"
                + "• Risque de brique : n'utilisez QUE les images du firmware EXACT de votre modèle.\n"
                + "• RootWizard est prévu pour votre propre appareil, dans un cadre légal.");

        p("honest_title", "Un mot d'honnêteté");
        p("honest_body",
                "Aucune application ne peut rooter Android 15 « en un vrai clic » : le système est "
                + "verrouillé et il n'existe pas d'exploit universel public. Les APK qui promettent "
                + "l'inverse sont des arnaques ou des malwares. RootWizard fait tout ce qu'une APK "
                + "peut faire : détection de l'état du téléphone, bons réglages, liens officiels et "
                + "commandes exactes prêtes à copier. Deux étapes demandent un PC (déverrouillage + "
                + "flash), ou un second téléphone en OTG (app Bugjaeger).");

        p("device_title", "Votre appareil");
        p("device_model", "Modèle");
        p("device_android", "Système");
        p("device_rom", "ROM");
        p("device_soc", "Puce (SoC)");

        p("status_title", "État actuel");
        p("st_boot", "Bootloader");
        p("st_oem", "Déverrouillage OEM");
        p("st_adb", "Débogage USB");
        p("st_magisk", "App Magisk");
        p("st_root", "Root actif");
        p("btn_refresh", "Rafraîchir l'état");

        p("val_unknown", "Inconnu");
        p("val_locked", "Verrouillé");
        p("val_unlocked", "DÉVERROUILLÉ");
        p("val_yellow", "Verrouillé (ROM custom)");
        p("val_red", "Corrompu / altéré");
        p("val_on", "Activé");
        p("val_off", "Désactivé");
        p("val_installed", "Installé");
        p("val_missing", "Absent");
        p("val_checking", "Test en cours…");
        p("val_yes", "OUI");

        p("s0_t", "Étape 0 · Conditions & responsabilité");
        p("s0_b",
                "Procédure générique AOSP (ex. Blackview Xplore1 · DokeOS 4.2 / Android 15), sur VOTRE propre appareil.\n"
                + "1. Le déverrouillage du bootloader efface tout : c'est irréversible.\n"
                + "2. Garantie : le fabricant peut refuser la prise en charge.\n"
                + "3. Play Integrity : apps bancaires, Google Wallet, certains jeux peuvent ne plus démarrer.\n"
                + "4. Brique possible si l'image flashée ne correspond pas à votre build exact.\n"
                + "Continuez uniquement si vous acceptez ces points.");

        p("s1_t", "Étape 1 · Sauvegarde");
        p("s1_b",
                "Le téléphone sera remis à zéro à l'étape 3. Sauvegardez avant de continuer : photos, "
                + "contacts, codes 2FA, conversations (export local), et notez vos comptes. Un backup "
                + "PC (adb pull) des dossiers importants est recommandé.");

        p("s2_t", "Étape 2 · Options développeur");
        p("s2_b",
                "Paramètres → À propos du téléphone → tapoter 7 fois sur « Numéro de build ».\n"
                + "Puis dans Options développeur : activer « Déverrouillage OEM » ET « Débogage USB ».\n"
                + "Les deux doivent être « Activé » dans le panneau État ci-dessus.");

        p("s3_t", "Étape 3 · Déverrouiller le bootloader (PC)");
        p("s3_b",
                "Depuis un PC avec les platform-tools (adb + fastboot) :\n"
                + "1. adb reboot bootloader\n"
                + "2. fastboot devices   (vérifie que le téléphone est détecté)\n"
                + "3. fastboot flashing unlock\n"
                + "Confirmez sur l'écran du téléphone (Volume = naviguer, Power = valider). "
                + "Toutes les données seront effacées.\n"
                + "Sans PC : un second téléphone + l'app « Bugjaeger Mobile ADB » peut envoyer du "
                + "fastboot en OTG.\n"
                + "Plan B MediaTek : mtkclient (exploit BROM) peut déverrouiller/flasher beaucoup de "
                + "puces MT6xxx même verrouillées, depuis un PC Linux/Windows.");

        p("cmds_3", "adb reboot bootloader\nfastboot devices\nfastboot flashing unlock");

        p("s4_t", "Étape 4 · Récupérer init_boot.img");
        p("s4_b",
                "Sur Android 15, le ramdisk est dans la partition init_boot (plus dans boot.img).\n"
                + "1. Téléchargez le firmware EXACT de votre Xplore1 (même build DokeOS) sur le site "
                + "Blackview, ou demandez-le au support avec le n° de série.\n"
                + "2. OTA zip : extrayez payload.bin avec payload-dumper-go, puis init_boot.img.\n"
                + "3. Firmware complet MTK (fichiers scatter) : SP Flash Tool ou mtkclient pour lire "
                + "init_boot.img.\n"
                + "N'utilisez JAMAIS l'init_boot d'un autre modèle ou d'un autre build.");

        p("s5_t", "Étape 5 · Patcher avec Magisk");
        p("s5_b",
                "1. Installez Magisk v27 ou plus récent (compatible Android 15).\n"
                + "2. Magisk → Installer → « Sélectionner et patcher un fichier » → init_boot.img.\n"
                + "3. Magisk crée Download/magisk_patched-XXXXX.img → copiez-le sur le PC (adb pull ou câble).");

        p("s6_t", "Étape 6 · Flasher l'image patchée");
        p("s6_b",
                "Depuis le PC :\n"
                + "1. adb reboot bootloader\n"
                + "2. fastboot flash init_boot magisk_patched-XXXXX.img\n"
                + "3. fastboot reboot\n"
                + "(Si votre firmware n'a pas de partition init_boot : fastboot flash boot "
                + "magisk_patched-XXXXX.img)\n"
                + "Le premier redémarrage est plus long : c'est normal.");

        p("cmds_6", "adb reboot bootloader\nfastboot flash init_boot magisk_patched-XXXXX.img\nfastboot reboot");

        p("s7_t", "Étape 7 · Vérifier le root");
        p("s7_b",
                "Ouvrez Magisk : « Installé : <version> » doit s'afficher. Testez avec le bouton "
                + "ci-dessous.\n"
                + "Une fois rooté, l'APK AndroMITM pourra piloter bettercap directement sur ce "
                + "téléphone (ip_forward, arp.spoof, net.sniff…).\n"
                + "Optionnel : Zygisk + module « Play Integrity Fix » pour les apps bancaires "
                + "(documentation XDA, à vos risques).");

        p("footer",
                "RootWizard fait partie de la suite AndroMITM. Usage légal : uniquement l'appareil "
                + "qui vous appartient et dont vous avez l'autorisation. AndroMITM n'est pas affilié "
                + "à Blackview ni au projet Magisk. Marques et produits : propriété de leurs "
                + "propriétaires respectifs.");

        p("soc_mtk",
                "Puce MediaTek détectée : voie fastboot classique ci-dessus ; plan B mtkclient " +
                "(mode BROM) pour lire/écrire init_boot même bootloader verrouillé (majorité des MT6xxx).");
        p("soc_unisoc",
                "Puce Unisoc détectée : extraction init_boot via SPD Research/Upgrade Tool ; " +
                "fastboot reste la voie principale.");
        p("soc_qcom",
                "Puce Qualcomm détectée : fastboot standard ; dernier recours EDL (mode 9008) avec " +
                "les programmeurs du firmware.");
        p("soc_other", "Puce non identifiée : suivez la voie fastboot standard.");

        p("btn_open_dev", "Ouvrir les options développeur");
        p("btn_copy", "Copier les commandes");
        p("btn_blackview", "Ouvrir le site Blackview (firmware)");
        p("btn_magisk", "Télécharger Magisk (GitHub)");
        p("btn_open_magisk", "Ouvrir l'app Magisk");
        p("btn_test_root", "Tester le root maintenant");
        p("toast_copied", "Commandes copiées dans le presse-papiers");
        p("toast_root_ok", "Root actif (uid=0)");
        p("toast_no_app", "App/Page introuvable sur cet appareil");

        // ═══ Profil Xiaomi / Redmi (HyperOS) ═══
        p("x0_t", "Étape 0 · Conditions & responsabilité (Xiaomi)");
        p("x0_b",
                "Procédure pour Redmi Note 14 / Xiaomi / POCO (HyperOS), sur VOTRE propre appareil.\n"
                + "1. Le déverrouillage Xiaomi EFFACE TOUTES LES DONNÉES.\n"
                + "2. Il faut un compte Mi ET un délai d'attente officiel (souvent 7 jours, parfois 30+ selon le compte).\n"
                + "3. Un PC Windows est requis pour Mi Unlock Tool (outil officiel).\n"
                + "4. Garantie : le fabricant peut refuser la prise en charge ; Play Integrity : apps bancaires/Wallet possiblement bloquées.\n"
                + "5. Brique possible avec une image d'un autre modèle/build. Continuez seulement si vous acceptez.");

        p("x1_t", "Étape 1 · Sauvegarde");
        p("x1_b",
                "Le déverrouillage Xiaomi remet le téléphone à zéro (étape 4). Sauvegardez photos, "
                + "contacts, codes 2FA, conversations (export local). Un adb pull des dossiers "
                + "importants est recommandé. Notez aussi votre variante exacte (Redmi Note 14 4G/5G, zone) ; elle détermine la fastboot ROM.");

        p("x2_t", "Étape 2 · Compte Mi & options développeur");
        p("x2_b",
                "1. Ajoutez (ou créez) votre compte Mi dans Paramètres, avec une carte SIM active dans le téléphone.\n"
                + "2. Paramètres → À propos du téléphone → tapoter 7× sur « Version HyperOS/OS » pour activer les options développeur.\n"
                + "3. Dans Options développeur : activer « Déverrouillage OEM », « Débogage USB » et "
                + "« Débogage USB (Paramètres de sécurité) ». Tout doit être « Activé » dans le panneau État.");

        p("x3_t", "Étape 3 · Lier le compte Mi Unlock & attendre");
        p("x3_b",
                "Options développeur → « Statut Mi Unlock » → « Lier le compte au dispositif » "
                + "(nécessite SIM + réseau).\n"
                + "Ensuite, ATTENDEZ le délai imposé par Xiaomi (souvent 7 jours ; parfois 30, 90 ou plus "
                + "selon l'ancienneté du compte). Le message exact s'affichera à l'étape 4.\n"
                + "Astuce : laissez la SIM et le compte en place pendant toute la période d'attente.");

        p("x4_t", "Étape 4 · Déverrouiller (PC Windows + Mi Unlock Tool)");
        p("x4_b",
                "1. Sur un PC Windows : téléchargez Mi Unlock Tool (miflash_unlock) depuis la page officielle et connectez-vous avec votre compte Mi.\n"
                + "2. Téléphone en mode bootloader : adb reboot bootloader\n"
                + "3. Branchez le téléphone, l'outil détecte le compte lié → bouton Unlock → confirmez.\n"
                + "4. Effacement complet automatique. Si message « Can't unlock… try after X days » : "
                + "attendez le délai puis recommencez l'étape 4 (pas besoin de re-lier).\n"
                + "L'outil est officiellement Windows uniquement (VM possible mais non garantie).");

        p("x5_t", "Étape 5 · Récupérer init_boot.img (fastboot ROM)");
        p("x5_b",
                "1. Téléchargez la fastboot ROM EXACTE de votre Redmi Note 14 (variante 4G/5G + zone + "
                + "même build HyperOS) — pack officiel .tgz ou archives communautaires réputées.\n"
                + "2. Décompressez le .tgz (7-Zip) : cherchez init_boot.img.\n"
                + "3. Si le pack n'a pas d'init_boot.img mais un boot.img : c'est boot.img qui sera patché (étapes 6-7).\n"
                + "4. Gardez aussi vbmeta.img du pack de côté (dépannage bootloop, étape 7).");

        p("x6_t", "Étape 6 · Patcher avec Magisk");
        p("x6_b",
                "1. Installez Magisk v27 ou plus récent.\n"
                + "2. Magisk → Installer → « Sélectionner et patcher un fichier » → init_boot.img (ou boot.img).\n"
                + "3. Récupérez le résultat sur le PC : adb pull Download/magisk_patched-XXXXX.img");

        p("x7_t", "Étape 7 · Flasher");
        p("x7_b",
                "Depuis le PC :\n"
                + "1. adb reboot bootloader\n"
                + "2. fastboot flash init_boot magisk_patched-XXXXX.img   (ou : fastboot flash boot magisk_patched-XXXXX.img si pas d'init_boot)\n"
                + "3. fastboot reboot\n"
                + "Premier démarrage plus long ; l'avertissement « bootloader unlocked » au démarrage est normal.\n"
                + "En cas de bootloop : reflashez le vbmeta du pack : fastboot flash vbmeta vbmeta.img --disable-verity --disable-verification (appareil déjà déverrouillé).");

        p("x8_t", "Étape 8 · Vérifier le root");
        p("x8_b",
                "Ouvrez Magisk : « Installé : <version> » doit s'afficher. Testez avec le bouton ci-dessous.\n"
                + "Une fois rooté, l'APK AndroMITM pourra piloter bettercap directement sur ce téléphone "
                + "(ip_forward, arp.spoof, net.sniff…).\n"
                + "Optionnel : Zygisk + module « Play Integrity Fix » pour les apps bancaires (XDA, à vos risques).");
    }

    // ════════════════════════════════ EN ════════════════════════════════
    private static void en() {
        p("app_title", "AndroMITM RootWizard");
        p("app_sub", "Guided rooting assistant — Blackview Xplore1 · Redmi Note 14 · Android 14/15");

        p("p_title", "Device profile");
        p("p_auto", "Auto");
        p("p_std", "Blackview / AOSP");
        p("p_xiaomi", "Xiaomi / Redmi");
        p("btn_miunlock", "Open official Mi Unlock page");
        p("btn_fw_xiaomi", "Xiaomi/HyperOS firmwares (archives)");

        p("warn_title", "Read this first");
        p("warn_body",
                "• Unlocking the bootloader WIPES ALL DATA on the phone.\n"
                + "• Warranty may be voided.\n"
                + "• Banking apps and Google Wallet may stop working (Play Integrity).\n"
                + "• Brick risk: only use images from the EXACT firmware of your model.\n"
                + "• RootWizard is meant for your own device, within the law.");

        p("honest_title", "Honesty first");
        p("honest_body",
                "No app can truly one-click root Android 15: the system is locked and there is no "
                + "public universal exploit. APKs claiming otherwise are scams or malware. RootWizard "
                + "does everything an app can do: detects your phone state, provides the right "
                + "settings, official links and exact copy-paste commands. Two steps need a PC "
                + "(unlock + flash), or a second phone over OTG (Bugjaeger app).");

        p("device_title", "Your device");
        p("device_model", "Model");
        p("device_android", "System");
        p("device_rom", "ROM");
        p("device_soc", "Chipset (SoC)");

        p("status_title", "Current state");
        p("st_boot", "Bootloader");
        p("st_oem", "OEM unlocking");
        p("st_adb", "USB debugging");
        p("st_magisk", "Magisk app");
        p("st_root", "Root active");
        p("btn_refresh", "Refresh state");

        p("val_unknown", "Unknown");
        p("val_locked", "Locked");
        p("val_unlocked", "UNLOCKED");
        p("val_yellow", "Locked (custom ROM)");
        p("val_red", "Corrupted / tampered");
        p("val_on", "Enabled");
        p("val_off", "Disabled");
        p("val_installed", "Installed");
        p("val_missing", "Missing");
        p("val_checking", "Testing…");
        p("val_yes", "YES");

        p("s0_t", "Step 0 · Conditions & responsibility");
        p("s0_b",
                "Procedure for the Blackview Xplore1 (DokeOS 4.2 / Android 15), on YOUR own device.\n"
                + "1. Unlocking the bootloader wipes everything: irreversible.\n"
                + "2. Warranty: Blackview may refuse service.\n"
                + "3. Play Integrity: banking apps, Google Wallet, some games may stop working.\n"
                + "4. Brick risk if the flashed image does not match your exact build.\n"
                + "Continue only if you accept these points.");

        p("s1_t", "Step 1 · Backup");
        p("s1_b",
                "The phone will be factory-reset at step 3. Back up first: photos, contacts, 2FA "
                + "codes, conversations (local export). An adb pull of important folders is recommended.");

        p("s2_t", "Step 2 · Developer options");
        p("s2_b",
                "Settings → About phone → tap « Build number » 7 times.\n"
                + "Then in Developer options: enable « OEM unlocking » AND « USB debugging ».\n"
                + "Both must show « Enabled » in the State panel above.");

        p("s3_t", "Step 3 · Unlock the bootloader (PC)");
        p("s3_b",
                "From a PC with platform-tools (adb + fastboot):\n"
                + "1. adb reboot bootloader\n"
                + "2. fastboot devices   (checks the phone is detected)\n"
                + "3. fastboot flashing unlock\n"
                + "Confirm on the phone screen (Volume = navigate, Power = confirm). All data will "
                + "be wiped.\n"
                + "Without a PC: a second phone + « Bugjaeger Mobile ADB » can send fastboot over OTG.\n"
                + "MediaTek plan B: mtkclient (BROM exploit) can unlock/flash many MT6xxx chips even "
                + "locked, from a Linux/Windows PC.");

        p("cmds_3", "adb reboot bootloader\nfastboot devices\nfastboot flashing unlock");

        p("s4_t", "Step 4 · Get init_boot.img");
        p("s4_b",
                "On Android 15, the ramdisk lives in the init_boot partition (not boot.img).\n"
                + "1. Download the EXACT firmware for your Xplore1 (same DokeOS build) from the "
                + "Blackview site, or request it from support with the serial number.\n"
                + "2. OTA zip: extract payload.bin with payload-dumper-go, then init_boot.img.\n"
                + "3. Full MTK firmware (scatter files): SP Flash Tool or mtkclient to read "
                + "init_boot.img.\n"
                + "NEVER use an init_boot from another model or build.");

        p("s5_t", "Step 5 · Patch with Magisk");
        p("s5_b",
                "1. Install Magisk v27+ (Android 15 compatible).\n"
                + "2. Magisk → Install → « Select and Patch a File » → init_boot.img.\n"
                + "3. Magisk creates Download/magisk_patched-XXXXX.img → copy it to the PC.");

        p("s6_t", "Step 6 · Flash the patched image");
        p("s6_b",
                "From the PC:\n"
                + "1. adb reboot bootloader\n"
                + "2. fastboot flash init_boot magisk_patched-XXXXX.img\n"
                + "3. fastboot reboot\n"
                + "(If your firmware has no init_boot partition: fastboot flash boot "
                + "magisk_patched-XXXXX.img)\n"
                + "First boot takes longer: that is normal.");

        p("cmds_6", "adb reboot bootloader\nfastboot flash init_boot magisk_patched-XXXXX.img\nfastboot reboot");

        p("s7_t", "Step 7 · Verify root");
        p("s7_b",
                "Open Magisk: « Installed: <version> » must appear. Test with the button below.\n"
                + "Once rooted, the AndroMITM APK can drive bettercap directly on this phone "
                + "(ip_forward, arp.spoof, net.sniff…).\n"
                + "Optional: Zygisk + « Play Integrity Fix » module for banking apps (XDA docs, at "
                + "your own risk).");

        p("footer",
                "RootWizard is part of the AndroMITM suite. Legal use: only devices you own or are "
                + "authorized to test. AndroMITM is not affiliated with Blackview or the Magisk "
                + "project. Product names belong to their respective owners.");

        p("soc_mtk",
                "MediaTek chip detected: standard fastboot path above; plan B mtkclient (BROM mode) " +
                "to read/write init_boot even with a locked bootloader (most MT6xxx).");
        p("soc_unisoc",
                "Unisoc chip detected: init_boot extraction via SPD Research/Upgrade Tool; fastboot " +
                "remains the main path.");
        p("soc_qcom",
                "Qualcomm chip detected: standard fastboot; last resort EDL (9008 mode) with the " +
                "firmware programmers.");
        p("soc_other", "Chipset not identified: follow the standard fastboot path.");

        p("btn_open_dev", "Open developer options");
        p("btn_copy", "Copy commands");
        p("btn_blackview", "Open Blackview site (firmware)");
        p("btn_magisk", "Download Magisk (GitHub)");
        p("btn_open_magisk", "Open Magisk app");
        p("btn_test_root", "Test root now");
        p("toast_copied", "Commands copied to clipboard");
        p("toast_root_ok", "Root active (uid=0)");
        p("toast_no_app", "App/Screen not found on this device");

        // ═══ Xiaomi / Redmi (HyperOS) profile ═══
        p("x0_t", "Step 0 · Conditions & responsibility (Xiaomi)");
        p("x0_b",
                "Procedure for Redmi Note 14 / Xiaomi / POCO (HyperOS), on YOUR own device.\n"
                + "1. Xiaomi unlocking WIPES ALL DATA.\n"
                + "2. You need a Mi account AND the official waiting period (usually 7 days, sometimes 30+ depending on the account).\n"
                + "3. A Windows PC is required for Mi Unlock Tool (official tool).\n"
                + "4. Warranty may be voided; Play Integrity: banking/Wallet apps may stop working.\n"
                + "5. Brick risk with an image from another model/build. Continue only if you accept.");

        p("x1_t", "Step 1 · Backup");
        p("x1_b",
                "Xiaomi unlocking factory-resets the phone (step 4). Back up photos, contacts, 2FA "
                + "codes, conversations (local export). An adb pull is recommended. Note your exact "
                + "variant (Redmi Note 14 4G/5G, region): it determines the fastboot ROM.");

        p("x2_t", "Step 2 · Mi account & developer options");
        p("x2_b",
                "1. Add (or create) your Mi account in Settings, with an active SIM card in the phone.\n"
                + "2. Settings → About phone → tap « HyperOS/OS version » 7 times to unlock developer options.\n"
                + "3. In Developer options: enable « OEM unlocking », « USB debugging » and "
                + "« USB debugging (Security settings) ». Everything must show « Enabled » in the State panel.");

        p("x3_t", "Step 3 · Bind Mi Unlock account & wait");
        p("x3_b",
                "Developer options → « Mi Unlock status » → « Add account and device » (SIM + network "
                + "required).\n"
                + "Then WAIT for the Xiaomi-imposed delay (usually 7 days; sometimes 30, 90 or more "
                + "depending on account age). The exact message will show at step 4.\n"
                + "Tip: keep the SIM and account in place during the whole waiting period.");

        p("x4_t", "Step 4 · Unlock (Windows PC + Mi Unlock Tool)");
        p("x4_b",
                "1. On a Windows PC: download Mi Unlock Tool (miflash_unlock) from the official page and sign in with your Mi account.\n"
                + "2. Phone in bootloader mode: adb reboot bootloader\n"
                + "3. Plug the phone, the tool detects the bound account → Unlock button → confirm.\n"
                + "4. Full automatic wipe. If it says « Can't unlock… try after X days »: wait, then "
                + "redo step 4 (no need to re-bind).\n"
                + "The tool is officially Windows-only (a VM may work but is not guaranteed).");

        p("x5_t", "Step 5 · Get init_boot.img (fastboot ROM)");
        p("x5_b",
                "1. Download the EXACT fastboot ROM for your Redmi Note 14 (4G/5G variant + region + "
                + "same HyperOS build) — official .tgz package or reputable community archives.\n"
                + "2. Extract the .tgz (7-Zip): look for init_boot.img.\n"
                + "3. If the pack has no init_boot.img but a boot.img: boot.img will be patched (steps 6-7).\n"
                + "4. Keep vbmeta.img from the pack aside (bootloop recovery, step 7).");

        p("x6_t", "Step 6 · Patch with Magisk");
        p("x6_b",
                "1. Install Magisk v27 or newer.\n"
                + "2. Magisk → Install → « Select and Patch a File » → init_boot.img (or boot.img).\n"
                + "3. Get the result on the PC: adb pull Download/magisk_patched-XXXXX.img");

        p("x7_t", "Step 7 · Flash");
        p("x7_b",
                "From the PC:\n"
                + "1. adb reboot bootloader\n"
                + "2. fastboot flash init_boot magisk_patched-XXXXX.img   (or: fastboot flash boot magisk_patched-XXXXX.img if no init_boot)\n"
                + "3. fastboot reboot\n"
                + "First boot takes longer; the « bootloader unlocked » warning at boot is normal.\n"
                + "If bootloop: reflash the pack's vbmeta: fastboot flash vbmeta vbmeta.img --disable-verity --disable-verification (device already unlocked).");

        p("x8_t", "Step 8 · Verify root");
        p("x8_b",
                "Open Magisk: « Installed: <version> » must appear. Test with the button below.\n"
                + "Once rooted, the AndroMITM APK can drive bettercap directly on this phone "
                + "(ip_forward, arp.spoof, net.sniff…).\n"
                + "Optional: Zygisk + « Play Integrity Fix » module for banking apps (XDA, at your own risk).");
    }
}
