// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.utils;

import static io.smallrye.common.constraint.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
public class StringFormatUtilsTest {

  @Nested
  public class EncodeToUtf8Tests {

    @Test
    @DisplayName("Encodes a string with basic latin chars")
    public void encodeToUtf8BasicLatinStringTest() {
      String toEncode = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
      String hoped = "\\u0021\\u0022\\u0023\\u0024\\u0025\\u0026\\u0027\\u0028\\u0029\\u002a\\u002b\\u002c\\u002d\\u002e\\u002f\\u0030\\u0031\\u0032\\u0033\\u0034\\u0035\\u0036\\u0037\\u0038\\u0039\\u003a\\u003b\\u003c\\u003d\\u003e\\u003f\\u0040\\u0041\\u0042\\u0043\\u0044\\u0045\\u0046\\u0047\\u0048\\u0049\\u004a\\u004b\\u004c\\u004d\\u004e\\u004f\\u0050\\u0051\\u0052\\u0053\\u0054\\u0055\\u0056\\u0057\\u0058\\u0059\\u005a\\u005b\\u005c\\u005d\\u005e\\u005f\\u0060\\u0061\\u0062\\u0063\\u0064\\u0065\\u0066\\u0067\\u0068\\u0069\\u006a\\u006b\\u006c\\u006d\\u006e\\u006f\\u0070\\u0071\\u0072\\u0073\\u0074\\u0075\\u0076\\u0077\\u0078\\u0079\\u007a\\u007b\\u007c\\u007d\\u007e";
      assertEquals(hoped, StringFormatUtils.encodeToUtf8(toEncode));
    }

    @Test
    @DisplayName("Encodes a string with supplement latin chars")
    public void encodeToUtf8SupplementLatinStringTest() {
      String toEncode = "¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";
      String hoped = "\\u00a1\\u00a2\\u00a3\\u00a4\\u00a5\\u00a6\\u00a7\\u00a8\\u00a9\\u00aa\\u00ab\\u00ac\\u00ae\\u00af\\u00b0\\u00b1\\u00b2\\u00b3\\u00b4\\u00b5\\u00b6\\u00b7\\u00b8\\u00b9\\u00ba\\u00bb\\u00bc\\u00bd\\u00be\\u00bf\\u00c0\\u00c1\\u00c2\\u00c3\\u00c4\\u00c5\\u00c6\\u00c7\\u00c8\\u00c9\\u00ca\\u00cb\\u00cc\\u00cd\\u00ce\\u00cf\\u00d0\\u00d1\\u00d2\\u00d3\\u00d4\\u00d5\\u00d6\\u00d7\\u00d8\\u00d9\\u00da\\u00db\\u00dc\\u00dd\\u00de\\u00df\\u00e0\\u00e1\\u00e2\\u00e3\\u00e4\\u00e5\\u00e6\\u00e7\\u00e8\\u00e9\\u00ea\\u00eb\\u00ec\\u00ed\\u00ee\\u00ef\\u00f0\\u00f1\\u00f2\\u00f3\\u00f4\\u00f5\\u00f6\\u00f7\\u00f8\\u00f9\\u00fa\\u00fb\\u00fc\\u00fd\\u00fe\\u00ff";
      assertEquals(hoped, StringFormatUtils.encodeToUtf8(toEncode));
    }

    @Test
    @DisplayName("Encodes a string with greek and coptic chars")
    public void encodeToUtf8GreekAndCopticStringTest() {
      String toEncode = "ʹ͵ͺ;΄΅Ά·ΈΉΊΌΎΏΐΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩΪΫάέήίΰαβγδεζηθικλμνξοπρςστυφχψωϊϋόύώϐϑϒϓϔϕϖϗϘϙϚϛϜϝϞϟϠϡϢϣϤϥϦϧϨϩϪϫϬϭϮϯϰϱϲϳϴϵ϶";
      String hoped = "\\u0374\\u0375\\u037a\\u037e\\u0384\\u0385\\u0386\\u0387\\u0388\\u0389\\u038a\\u038c\\u038e\\u038f\\u0390\\u0391\\u0392\\u0393\\u0394\\u0395\\u0396\\u0397\\u0398\\u0399\\u039a\\u039b\\u039c\\u039d\\u039e\\u039f\\u03a0\\u03a1\\u03a3\\u03a4\\u03a5\\u03a6\\u03a7\\u03a8\\u03a9\\u03aa\\u03ab\\u03ac\\u03ad\\u03ae\\u03af\\u03b0\\u03b1\\u03b2\\u03b3\\u03b4\\u03b5\\u03b6\\u03b7\\u03b8\\u03b9\\u03ba\\u03bb\\u03bc\\u03bd\\u03be\\u03bf\\u03c0\\u03c1\\u03c2\\u03c3\\u03c4\\u03c5\\u03c6\\u03c7\\u03c8\\u03c9\\u03ca\\u03cb\\u03cc\\u03cd\\u03ce\\u03d0\\u03d1\\u03d2\\u03d3\\u03d4\\u03d5\\u03d6\\u03d7\\u03d8\\u03d9\\u03da\\u03db\\u03dc\\u03dd\\u03de\\u03df\\u03e0\\u03e1\\u03e2\\u03e3\\u03e4\\u03e5\\u03e6\\u03e7\\u03e8\\u03e9\\u03ea\\u03eb\\u03ec\\u03ed\\u03ee\\u03ef\\u03f0\\u03f1\\u03f2\\u03f3\\u03f4\\u03f5\\u03f6";
      assertEquals(hoped, StringFormatUtils.encodeToUtf8(toEncode));
    }

    @Test
    @DisplayName("Encodes a string with Cyrillic chars")
    public void encodeToUtf8CyrillicStringTest() {
      String toEncode = "ЀЁЂЃЄЅІЇЈЉЊЋЌЍЎЏАБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдежзийклмнопрстуфхцчшщъыьэюяѐёђѓєѕіїјљњћќѝўџѠѡѢѣѤѥѦѧѨѩѪѫѬѭѮѯѰѱѲѳѴѵѶѷѸѹѺѻѼѽѾѿ";
      String hoped = "\\u0400\\u0401\\u0402\\u0403\\u0404\\u0405\\u0406\\u0407\\u0408\\u0409\\u040a\\u040b\\u040c\\u040d\\u040e\\u040f\\u0410\\u0411\\u0412\\u0413\\u0414\\u0415\\u0416\\u0417\\u0418\\u0419\\u041a\\u041b\\u041c\\u041d\\u041e\\u041f\\u0420\\u0421\\u0422\\u0423\\u0424\\u0425\\u0426\\u0427\\u0428\\u0429\\u042a\\u042b\\u042c\\u042d\\u042e\\u042f\\u0430\\u0431\\u0432\\u0433\\u0434\\u0435\\u0436\\u0437\\u0438\\u0439\\u043a\\u043b\\u043c\\u043d\\u043e\\u043f\\u0440\\u0441\\u0442\\u0443\\u0444\\u0445\\u0446\\u0447\\u0448\\u0449\\u044a\\u044b\\u044c\\u044d\\u044e\\u044f\\u0450\\u0451\\u0452\\u0453\\u0454\\u0455\\u0456\\u0457\\u0458\\u0459\\u045a\\u045b\\u045c\\u045d\\u045e\\u045f\\u0460\\u0461\\u0462\\u0463\\u0464\\u0465\\u0466\\u0467\\u0468\\u0469\\u046a\\u046b\\u046c\\u046d\\u046e\\u046f\\u0470\\u0471\\u0472\\u0473\\u0474\\u0475\\u0476\\u0477\\u0478\\u0479\\u047a\\u047b\\u047c\\u047d\\u047e\\u047f";
      assertEquals(hoped, StringFormatUtils.encodeToUtf8(toEncode));
    }

    @Test
    @DisplayName("Encodes a string with Armenian chars")
    public void encodeToUtf8ArmenianStringTest() {
      String toEncode = "ԱԲԳԴԵԶԷԸԹԺԻԼԽԾԿՀՁՂՃՄՅՆՇՈՉՊՋՌՍՎՏՐՑՒՓՔՕՖՙ՚՛՜՝՞՟աբգդեզէըթժիլխծկհձղճմյնշոչպջռսվտրցւփքօֆև։֊";
      String hoped = "\\u0531\\u0532\\u0533\\u0534\\u0535\\u0536\\u0537\\u0538\\u0539\\u053a\\u053b\\u053c\\u053d\\u053e\\u053f\\u0540\\u0541\\u0542\\u0543\\u0544\\u0545\\u0546\\u0547\\u0548\\u0549\\u054a\\u054b\\u054c\\u054d\\u054e\\u054f\\u0550\\u0551\\u0552\\u0553\\u0554\\u0555\\u0556\\u0559\\u055a\\u055b\\u055c\\u055d\\u055e\\u055f\\u0561\\u0562\\u0563\\u0564\\u0565\\u0566\\u0567\\u0568\\u0569\\u056a\\u056b\\u056c\\u056d\\u056e\\u056f\\u0570\\u0571\\u0572\\u0573\\u0574\\u0575\\u0576\\u0577\\u0578\\u0579\\u057a\\u057b\\u057c\\u057d\\u057e\\u057f\\u0580\\u0581\\u0582\\u0583\\u0584\\u0585\\u0586\\u0587\\u0589\\u058a";
      assertEquals(hoped, StringFormatUtils.encodeToUtf8(toEncode));
    }

    @Test
    @DisplayName("Encodes a string with arabic chars")
    public void encodeToUtf8ArabicStringTest() {
      String toEncode = "،؛؟ءآأؤإئابةتثجحخدذرزسشصضطظعغـفقكلمنهوىيًٌٍَُِّْٕٓٔ٠١٢٣٤٥٦٧٨٩٪٫٬٭ٮٯٰٱٲٳٴٵٶٷٸٹٺٻټٽپٿڀځڂڃڄڅچڇڈډڊڋڌڍڎڏڐڑڒړڔڕږڗژڙښڛڜڝڞڟڠڡڢڣڤڥڦڧڨکڪګڬ...";
      String hoped = "\\u060c\\u061b\\u061f\\u0621\\u0622\\u0623\\u0624\\u0625\\u0626\\u0627\\u0628\\u0629\\u062a\\u062b\\u062c\\u062d\\u062e\\u062f\\u0630\\u0631\\u0632\\u0633\\u0634\\u0635\\u0636\\u0637\\u0638\\u0639\\u063a\\u0640\\u0641\\u0642\\u0643\\u0644\\u0645\\u0646\\u0647\\u0648\\u0649\\u064a\\u064b\\u064c\\u064d\\u064e\\u064f\\u0650\\u0651\\u0652\\u0653\\u0654\\u0655\\u0660\\u0661\\u0662\\u0663\\u0664\\u0665\\u0666\\u0667\\u0668\\u0669\\u066a\\u066b\\u066c\\u066d\\u066e\\u066f\\u0670\\u0671\\u0672\\u0673\\u0674\\u0675\\u0676\\u0677\\u0678\\u0679\\u067a\\u067b\\u067c\\u067d\\u067e\\u067f\\u0680\\u0681\\u0682\\u0683\\u0684\\u0685\\u0686\\u0687\\u0688\\u0689\\u068a\\u068b\\u068c\\u068d\\u068e\\u068f\\u0690\\u0691\\u0692\\u0693\\u0694\\u0695\\u0696\\u0697\\u0698\\u0699\\u069a\\u069b\\u069c\\u069d\\u069e\\u069f\\u06a0\\u06a1\\u06a2\\u06a3\\u06a4\\u06a5\\u06a6\\u06a7\\u06a8\\u06a9\\u06aa\\u06ab\\u06ac\\u002e\\u002e\\u002e";
      assertEquals(hoped, StringFormatUtils.encodeToUtf8(toEncode));
    }

    @Test
    @DisplayName("Encodes a string with syriac chars")
    public void encodeToUtf8SyriacStringTest() {
      String toEncode = "܀܁܂܃܄܅܆܇܈܉܊܋܌܍\u070Fܐܑܒܓܔܕܖܗܘܙܚܛܜܝܞܟܠܡܢܣܤܥܦܧܨܩܪܫܬܱܴܷܸܹܻܼܾ݂݄݆݈ܰܲܳܵܶܺܽܿ݀݁݃݅݇݉݊";
      String hoped = "\\u0700\\u0701\\u0702\\u0703\\u0704\\u0705\\u0706\\u0707\\u0708\\u0709\\u070a\\u070b\\u070c\\u070d\\u070f\\u0710\\u0711\\u0712\\u0713\\u0714\\u0715\\u0716\\u0717\\u0718\\u0719\\u071a\\u071b\\u071c\\u071d\\u071e\\u071f\\u0720\\u0721\\u0722\\u0723\\u0724\\u0725\\u0726\\u0727\\u0728\\u0729\\u072a\\u072b\\u072c\\u0730\\u0731\\u0732\\u0733\\u0734\\u0735\\u0736\\u0737\\u0738\\u0739\\u073a\\u073b\\u073c\\u073d\\u073e\\u073f\\u0740\\u0741\\u0742\\u0743\\u0744\\u0745\\u0746\\u0747\\u0748\\u0749\\u074a";
      assertEquals(hoped, StringFormatUtils.encodeToUtf8(toEncode));
    }

    @Test
    @DisplayName("Encodes a string with bengali chars")
    public void encodeToUtf8BengaliStringTest() {
      String toEncode = "ঁংঃঅআইঈউঊঋঌএঐওঔকখগঘঙচছজঝঞটঠডঢণতথদধনপফবভমযরলশষসহ়ািীুূৃৄেৈোৌ্ৗড়ঢ়য়ৠৡৢৣ০১২৩৪৫৬৭৮৯ৰৱ৲৳৴৵৶৷৸৹৺";
      String hoped = "\\u0981\\u0982\\u0983\\u0985\\u0986\\u0987\\u0988\\u0989\\u098a\\u098b\\u098c\\u098f\\u0990\\u0993\\u0994\\u0995\\u0996\\u0997\\u0998\\u0999\\u099a\\u099b\\u099c\\u099d\\u099e\\u099f\\u09a0\\u09a1\\u09a2\\u09a3\\u09a4\\u09a5\\u09a6\\u09a7\\u09a8\\u09aa\\u09ab\\u09ac\\u09ad\\u09ae\\u09af\\u09b0\\u09b2\\u09b6\\u09b7\\u09b8\\u09b9\\u09bc\\u09be\\u09bf\\u09c0\\u09c1\\u09c2\\u09c3\\u09c4\\u09c7\\u09c8\\u09cb\\u09cc\\u09cd\\u09d7\\u09dc\\u09dd\\u09df\\u09e0\\u09e1\\u09e2\\u09e3\\u09e6\\u09e7\\u09e8\\u09e9\\u09ea\\u09eb\\u09ec\\u09ed\\u09ee\\u09ef\\u09f0\\u09f1\\u09f2\\u09f3\\u09f4\\u09f5\\u09f6\\u09f7\\u09f8\\u09f9\\u09fa";
      assertEquals(hoped, StringFormatUtils.encodeToUtf8(toEncode));
    }
  }

  @Nested
  public class DecodeFromUtf8Tests {

    @Test
    @DisplayName("Decodes a string with basic latin chars")
    public void decodeFromUtf8BasicLatinStringTest() throws Exception {
      String toDecode = "\\u0021\\u0022\\u0023\\u0024\\u0025\\u0026\\u0027\\u0028\\u0029\\u002a\\u002b\\u002c\\u002d\\u002e\\u002f\\u0030\\u0031\\u0032\\u0033\\u0034\\u0035\\u0036\\u0037\\u0038\\u0039\\u003a\\u003b\\u003c\\u003d\\u003e\\u003f\\u0040\\u0041\\u0042\\u0043\\u0044\\u0045\\u0046\\u0047\\u0048\\u0049\\u004a\\u004b\\u004c\\u004d\\u004e\\u004f\\u0050\\u0051\\u0052\\u0053\\u0054\\u0055\\u0056\\u0057\\u0058\\u0059\\u005a\\u005b\\u005c\\u005d\\u005e\\u005f\\u0060\\u0061\\u0062\\u0063\\u0064\\u0065\\u0066\\u0067\\u0068\\u0069\\u006a\\u006b\\u006c\\u006d\\u006e\\u006f\\u0070\\u0071\\u0072\\u0073\\u0074\\u0075\\u0076\\u0077\\u0078\\u0079\\u007a\\u007b\\u007c\\u007d\\u007e";
      String hoped = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
      assertEquals(hoped, StringFormatUtils.decodeFromUtf8(toDecode));
    }

    @Test
    @DisplayName("Decodes a string with supplement latin chars")
    public void decodeFromUtf8SupplementLatinStringTest() throws Exception {
      String toDecode = "\\u00a1\\u00a2\\u00a3\\u00a4\\u00a5\\u00a6\\u00a7\\u00a8\\u00a9\\u00aa\\u00ab\\u00ac\\u00ae\\u00af\\u00b0\\u00b1\\u00b2\\u00b3\\u00b4\\u00b5\\u00b6\\u00b7\\u00b8\\u00b9\\u00ba\\u00bb\\u00bc\\u00bd\\u00be\\u00bf\\u00c0\\u00c1\\u00c2\\u00c3\\u00c4\\u00c5\\u00c6\\u00c7\\u00c8\\u00c9\\u00ca\\u00cb\\u00cc\\u00cd\\u00ce\\u00cf\\u00d0\\u00d1\\u00d2\\u00d3\\u00d4\\u00d5\\u00d6\\u00d7\\u00d8\\u00d9\\u00da\\u00db\\u00dc\\u00dd\\u00de\\u00df\\u00e0\\u00e1\\u00e2\\u00e3\\u00e4\\u00e5\\u00e6\\u00e7\\u00e8\\u00e9\\u00ea\\u00eb\\u00ec\\u00ed\\u00ee\\u00ef\\u00f0\\u00f1\\u00f2\\u00f3\\u00f4\\u00f5\\u00f6\\u00f7\\u00f8\\u00f9\\u00fa\\u00fb\\u00fc\\u00fd\\u00fe\\u00ff";
      String hoped = "¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";
      assertEquals(hoped, StringFormatUtils.decodeFromUtf8(toDecode));
    }

    @Test
    @DisplayName("Decodes a string with greek and coptic chars")
    public void decodeFromUtf8GreekAndCopticStringTest() throws Exception {
      String toDecode = "\\u0374\\u0375\\u037a\\u037e\\u0384\\u0385\\u0386\\u0387\\u0388\\u0389\\u038a\\u038c\\u038e\\u038f\\u0390\\u0391\\u0392\\u0393\\u0394\\u0395\\u0396\\u0397\\u0398\\u0399\\u039a\\u039b\\u039c\\u039d\\u039e\\u039f\\u03a0\\u03a1\\u03a3\\u03a4\\u03a5\\u03a6\\u03a7\\u03a8\\u03a9\\u03aa\\u03ab\\u03ac\\u03ad\\u03ae\\u03af\\u03b0\\u03b1\\u03b2\\u03b3\\u03b4\\u03b5\\u03b6\\u03b7\\u03b8\\u03b9\\u03ba\\u03bb\\u03bc\\u03bd\\u03be\\u03bf\\u03c0\\u03c1\\u03c2\\u03c3\\u03c4\\u03c5\\u03c6\\u03c7\\u03c8\\u03c9\\u03ca\\u03cb\\u03cc\\u03cd\\u03ce\\u03d0\\u03d1\\u03d2\\u03d3\\u03d4\\u03d5\\u03d6\\u03d7\\u03d8\\u03d9\\u03da\\u03db\\u03dc\\u03dd\\u03de\\u03df\\u03e0\\u03e1\\u03e2\\u03e3\\u03e4\\u03e5\\u03e6\\u03e7\\u03e8\\u03e9\\u03ea\\u03eb\\u03ec\\u03ed\\u03ee\\u03ef\\u03f0\\u03f1\\u03f2\\u03f3\\u03f4\\u03f5\\u03f6";
      String hoped = "ʹ͵ͺ;΄΅Ά·ΈΉΊΌΎΏΐΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩΪΫάέήίΰαβγδεζηθικλμνξοπρςστυφχψωϊϋόύώϐϑϒϓϔϕϖϗϘϙϚϛϜϝϞϟϠϡϢϣϤϥϦϧϨϩϪϫϬϭϮϯϰϱϲϳϴϵ϶";
      assertEquals(hoped, StringFormatUtils.decodeFromUtf8(toDecode));
    }

    @Test
    @DisplayName("Decodes a string with Cyrillic chars")
    public void decodeFromUtf8CyrillicStringTest() throws Exception {
      String toDecode = "\\u0400\\u0401\\u0402\\u0403\\u0404\\u0405\\u0406\\u0407\\u0408\\u0409\\u040a\\u040b\\u040c\\u040d\\u040e\\u040f\\u0410\\u0411\\u0412\\u0413\\u0414\\u0415\\u0416\\u0417\\u0418\\u0419\\u041a\\u041b\\u041c\\u041d\\u041e\\u041f\\u0420\\u0421\\u0422\\u0423\\u0424\\u0425\\u0426\\u0427\\u0428\\u0429\\u042a\\u042b\\u042c\\u042d\\u042e\\u042f\\u0430\\u0431\\u0432\\u0433\\u0434\\u0435\\u0436\\u0437\\u0438\\u0439\\u043a\\u043b\\u043c\\u043d\\u043e\\u043f\\u0440\\u0441\\u0442\\u0443\\u0444\\u0445\\u0446\\u0447\\u0448\\u0449\\u044a\\u044b\\u044c\\u044d\\u044e\\u044f\\u0450\\u0451\\u0452\\u0453\\u0454\\u0455\\u0456\\u0457\\u0458\\u0459\\u045a\\u045b\\u045c\\u045d\\u045e\\u045f\\u0460\\u0461\\u0462\\u0463\\u0464\\u0465\\u0466\\u0467\\u0468\\u0469\\u046a\\u046b\\u046c\\u046d\\u046e\\u046f\\u0470\\u0471\\u0472\\u0473\\u0474\\u0475\\u0476\\u0477\\u0478\\u0479\\u047a\\u047b\\u047c\\u047d\\u047e\\u047f";
      String hoped = "ЀЁЂЃЄЅІЇЈЉЊЋЌЍЎЏАБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдежзийклмнопрстуфхцчшщъыьэюяѐёђѓєѕіїјљњћќѝўџѠѡѢѣѤѥѦѧѨѩѪѫѬѭѮѯѰѱѲѳѴѵѶѷѸѹѺѻѼѽѾѿ";
      assertEquals(hoped, StringFormatUtils.decodeFromUtf8(toDecode));
    }

    @Test
    @DisplayName("Decodes a string with Armenian chars")
    public void decodeFromUtf8ArmenianStringTest() throws Exception {
      String toDecode = "\\u0531\\u0532\\u0533\\u0534\\u0535\\u0536\\u0537\\u0538\\u0539\\u053a\\u053b\\u053c\\u053d\\u053e\\u053f\\u0540\\u0541\\u0542\\u0543\\u0544\\u0545\\u0546\\u0547\\u0548\\u0549\\u054a\\u054b\\u054c\\u054d\\u054e\\u054f\\u0550\\u0551\\u0552\\u0553\\u0554\\u0555\\u0556\\u0559\\u055a\\u055b\\u055c\\u055d\\u055e\\u055f\\u0561\\u0562\\u0563\\u0564\\u0565\\u0566\\u0567\\u0568\\u0569\\u056a\\u056b\\u056c\\u056d\\u056e\\u056f\\u0570\\u0571\\u0572\\u0573\\u0574\\u0575\\u0576\\u0577\\u0578\\u0579\\u057a\\u057b\\u057c\\u057d\\u057e\\u057f\\u0580\\u0581\\u0582\\u0583\\u0584\\u0585\\u0586\\u0587\\u0589\\u058a";
      String hoped = "ԱԲԳԴԵԶԷԸԹԺԻԼԽԾԿՀՁՂՃՄՅՆՇՈՉՊՋՌՍՎՏՐՑՒՓՔՕՖՙ՚՛՜՝՞՟աբգդեզէըթժիլխծկհձղճմյնշոչպջռսվտրցւփքօֆև։֊";
      assertEquals(hoped, StringFormatUtils.decodeFromUtf8(toDecode));
    }

    @Test
    @DisplayName("Decodes a string with arabic chars")
    public void decodeFromUtf8ArabicStringTest() throws Exception {
      String toDecode = "\\u060c\\u061b\\u061f\\u0621\\u0622\\u0623\\u0624\\u0625\\u0626\\u0627\\u0628\\u0629\\u062a\\u062b\\u062c\\u062d\\u062e\\u062f\\u0630\\u0631\\u0632\\u0633\\u0634\\u0635\\u0636\\u0637\\u0638\\u0639\\u063a\\u0640\\u0641\\u0642\\u0643\\u0644\\u0645\\u0646\\u0647\\u0648\\u0649\\u064a\\u064b\\u064c\\u064d\\u064e\\u064f\\u0650\\u0651\\u0652\\u0653\\u0654\\u0655\\u0660\\u0661\\u0662\\u0663\\u0664\\u0665\\u0666\\u0667\\u0668\\u0669\\u066a\\u066b\\u066c\\u066d\\u066e\\u066f\\u0670\\u0671\\u0672\\u0673\\u0674\\u0675\\u0676\\u0677\\u0678\\u0679\\u067a\\u067b\\u067c\\u067d\\u067e\\u067f\\u0680\\u0681\\u0682\\u0683\\u0684\\u0685\\u0686\\u0687\\u0688\\u0689\\u068a\\u068b\\u068c\\u068d\\u068e\\u068f\\u0690\\u0691\\u0692\\u0693\\u0694\\u0695\\u0696\\u0697\\u0698\\u0699\\u069a\\u069b\\u069c\\u069d\\u069e\\u069f\\u06a0\\u06a1\\u06a2\\u06a3\\u06a4\\u06a5\\u06a6\\u06a7\\u06a8\\u06a9\\u06aa\\u06ab\\u06ac\\u002e\\u002e\\u002e";
      String hoped = "،؛؟ءآأؤإئابةتثجحخدذرزسشصضطظعغـفقكلمنهوىيًٌٍَُِّْٕٓٔ٠١٢٣٤٥٦٧٨٩٪٫٬٭ٮٯٰٱٲٳٴٵٶٷٸٹٺٻټٽپٿڀځڂڃڄڅچڇڈډڊڋڌڍڎڏڐڑڒړڔڕږڗژڙښڛڜڝڞڟڠڡڢڣڤڥڦڧڨکڪګڬ...";
      assertEquals(hoped, StringFormatUtils.decodeFromUtf8(toDecode));
    }

    @Test
    @DisplayName("Decodes a string with syriac chars")
    public void decodeFromUtf8SyriacStringTest() throws Exception {
      String toDecode = "\\u0700\\u0701\\u0702\\u0703\\u0704\\u0705\\u0706\\u0707\\u0708\\u0709\\u070a\\u070b\\u070c\\u070d\\u070f\\u0710\\u0711\\u0712\\u0713\\u0714\\u0715\\u0716\\u0717\\u0718\\u0719\\u071a\\u071b\\u071c\\u071d\\u071e\\u071f\\u0720\\u0721\\u0722\\u0723\\u0724\\u0725\\u0726\\u0727\\u0728\\u0729\\u072a\\u072b\\u072c\\u0730\\u0731\\u0732\\u0733\\u0734\\u0735\\u0736\\u0737\\u0738\\u0739\\u073a\\u073b\\u073c\\u073d\\u073e\\u073f\\u0740\\u0741\\u0742\\u0743\\u0744\\u0745\\u0746\\u0747\\u0748\\u0749\\u074a";
      String hoped = "܀܁܂܃܄܅܆܇܈܉܊܋܌܍\u070Fܐܑܒܓܔܕܖܗܘܙܚܛܜܝܞܟܠܡܢܣܤܥܦܧܨܩܪܫܬܱܴܷܸܹܻܼܾ݂݄݆݈ܰܲܳܵܶܺܽܿ݀݁݃݅݇݉݊";
      assertEquals(hoped, StringFormatUtils.decodeFromUtf8(toDecode));
    }

    @Test
    @DisplayName("Decodes a string with bengali chars")
    public void decodeFromUtf8BengaliStringTest() throws Exception {
      String toDecode = "\\u0981\\u0982\\u0983\\u0985\\u0986\\u0987\\u0988\\u0989\\u098a\\u098b\\u098c\\u098f\\u0990\\u0993\\u0994\\u0995\\u0996\\u0997\\u0998\\u0999\\u099a\\u099b\\u099c\\u099d\\u099e\\u099f\\u09a0\\u09a1\\u09a2\\u09a3\\u09a4\\u09a5\\u09a6\\u09a7\\u09a8\\u09aa\\u09ab\\u09ac\\u09ad\\u09ae\\u09af\\u09b0\\u09b2\\u09b6\\u09b7\\u09b8\\u09b9\\u09bc\\u09be\\u09bf\\u09c0\\u09c1\\u09c2\\u09c3\\u09c4\\u09c7\\u09c8\\u09cb\\u09cc\\u09cd\\u09d7\\u09dc\\u09dd\\u09df\\u09e0\\u09e1\\u09e2\\u09e3\\u09e6\\u09e7\\u09e8\\u09e9\\u09ea\\u09eb\\u09ec\\u09ed\\u09ee\\u09ef\\u09f0\\u09f1\\u09f2\\u09f3\\u09f4\\u09f5\\u09f6\\u09f7\\u09f8\\u09f9\\u09fa";
      String hoped = "ঁংঃঅআইঈউঊঋঌএঐওঔকখগঘঙচছজঝঞটঠডঢণতথদধনপফবভমযরলশষসহ়ািীুূৃৄেৈোৌ্ৗড়ঢ়য়ৠৡৢৣ০১২৩৪৫৬৭৮৯ৰৱ৲৳৴৵৶৷৸৹৺";
      assertEquals(hoped, StringFormatUtils.decodeFromUtf8(toDecode));
    }
  }

  @Nested
  public class IsEncodedInUtf8Tests {

    @Test
    public void isEncodedInUtf8_okTest() {
      String str = "\\u0021\\u0022\\u0023\\u0024\\u0025\\u0026\\u0027\\u0028\\u0029\\u002a\\u002b\\u002c\\u002d\\u002e\\u002f\\u0030\\u0031\\u0032\\u0033\\u0034\\u0035\\u0036\\u0037\\u0038\\u0039\\u003a\\u003b\\u003c\\u003d\\u003e\\u003f\\u0040\\u0041\\u0042\\u0043\\u0044\\u0045\\u0046\\u0047\\u0048\\u0049\\u004a\\u004b\\u004c\\u004d\\u004e\\u004f\\u0050\\u0051\\u0052\\u0053\\u0054\\u0055\\u0056\\u0057\\u0058\\u0059\\u005a\\u005b\\u005c\\u005d\\u005e\\u005f\\u0060\\u0061\\u0062\\u0063\\u0064\\u0065\\u0066\\u0067\\u0068\\u0069\\u006a\\u006b\\u006c\\u006d\\u006e\\u006f\\u0070\\u0071\\u0072\\u0073\\u0074\\u0075\\u0076\\u0077\\u0078\\u0079\\u007a\\u007b\\u007c\\u007d\\u007e";
      assertTrue(StringFormatUtils.isEncodedInUtf8(str));
    }

    @Test
    public void isEncodedInUtf8_okFailed() {
      String str = "Hello";
      assertFalse(StringFormatUtils.isEncodedInUtf8(str));
    }
  }
}
