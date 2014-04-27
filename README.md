TreatLookaheadMatcher
=====================

TREAT regisztráció és használat:
================================

TreatRegistrarImpl.LookaheadToEngineConnector.Connect(engine, new LookaheadMatcherTreat(engine));
LookaheadMatcherTreat lmt = TreatRegistrarImpl.LookaheadToEngineConnector.GetLookaheadMatcherTreat(engine);
lmt.PowerTreatUp(RouteSensorQuerySpecification.instance());
Multiset<LookaheadMatching> result = lmt.matchThePattern(RouteSensorQuerySpecification.instance());

i. elem kivétele egy eredményből (csak index alapján támogatott az elem kivétele):
LookaheadMatching oneItem = result.iterator().next();
oneItem.get(i);

TREAT engine törlés (csak egyet):
---------------------------------

by LookaheadMatcherTreat:
TreatRegistrarImpl.LookaheadToEngineConnector.RemoveLookaheadMatcherTreat(LookaheadMatcherTreat lmt);

by IncQueryEngine:
TreatRegistrarImpl.LookaheadToEngineConnector.RemoveLookaheadMatcherTreat(IncQueryEngine engine);

TREAT engine törlés (minden engine-re minden LookaheadMatcherTreat-ot):
-----------------------------------------------------------------------
TreatRegistrarImpl.LookaheadToEngineConnector.Clean();


LookaheadMatcher
================

LookaheadMatcher útmutató:
LookaheadMatcherInterface való mindenre.

Inicializálás (NavigationHelper):
---------------------------------
ctor: LookaheadMatcherInterface(NavigationHelper navHelper)
InitializeAll(PQuery query, IncQueryEngine engine): inicializál az adott queryre mindent a NavigationHelper-be, végigmegy a minthaívási fán is (find/nac)
initialize(PQuery query): egy patternre inicializál.
Ha nincs inicializálva, akkor is detektálja és megteszi illesztés előtt a LookaheadMatcher.

Belső struktúra: AheadStructure (keresett, megtalált kényszerek, illeszkedések stb.)
Lekérése: PatternOnlyProcess(..)

Minta illesztése:
-----------------
Létezik-e? tryMatch(..)
Összes: matchAll(..)
Kiindulás rész-illeszkedésből (ahol már kényszerek is kielégítettek): searchChangesAll(..)

Ha néhány változó ismert, elég a matchAll(..)-t, vagy a tryMatch(..)-ot hívni, átadva az ismert értékeket.
pl.:
pattern Lista(A,B,C,D,E,F) = {..};
A kereső hívásakor a knownValues pedig pl.: new ArrayList<Object>(ismert, null, null, ismert2, ismert3, ismert4); Lényeg, hogy 'null' legyen ott, ahol nem ismerünk valamit.

Két interfész létezik, amit lehet implementálni, de létezik belső (vagy buta) megoldás:
IPartialPatternCacher: a hívott find/negfind mintákat lehet ebben cache-elni, így gyorsítva az illesztést. Buta implementációja: költségre végtelent becsül, felsorolás esetén mintailleszt nulláról
IConstraintEnumerator: a kényszereken iterálhatunk végig kedvünkre, a költségbecslésbe is belenyúlhatunk. A belső implementáció (ha null-t adnánk meg e helyére) eléggé jól működik.

Példa hívás:
------------
lookahead = new LookaheadMatcherInterface(engine.getBaseIndex());
lookahead.InitializeAll(RouteSensorQuerySpecification.instance(), engine);
Multiset<LookaheadMatching> result = lookahead.matchAll(engine, null, RouteSensorQuerySpecification.instance(), null, null);

A visszatérési érték itt is Multiset<LookaheadMatching>. Így:
i. elem kivétele egy eredményből (csak query paraméter index alapján támogatott az elem kivétele):
LookaheadMatching oneItem = result.iterator().next();
oneItem.get(i);