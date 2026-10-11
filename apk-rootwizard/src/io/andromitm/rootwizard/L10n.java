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
        p("app_sub", "Assistant de root guidé — Blackview Xplore1 · DokeOS 4.2 · Android 15");

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
                "Procédure pour le Blackview Xplore1 (DokeOS 4.2 / Android 15), sur VOTRE propre appareil.\n"
                + "1. Le déverrouillage du bootloader efface tout : c'est irréversible.\n"
                + "2. Compteur Knox-like / garantie : Blackview peut refuser la prise en charge.\n"
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
    }

    // ════════════════════════════════ EN ════════════════════════════════
    private static void en() {
        p("app_title", "AndroMITM RootWizard");
        p("app_sub", "Guided rooting assistant — Blackview Xplore1 · DokeOS 4.2 · Android 15");

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
    }
}
