package pe.profelab.core.model

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal as D

class WorkshopRulesTest {
    private fun equal(expected:String,actual:D?){assertNotNull(actual);assertEquals(0,D(expected).compareTo(requireNotNull(actual)))}
    @Test fun decimalCommaIsAcceptedWithoutAmbiguousThousands(){equal("1.5",InventoryRules.decimal("1,5"));assertNull(InventoryRules.decimal("1,000.5"));assertNull(InventoryRules.decimal("-2"));assertNull(InventoryRules.decimal("NaN"))}
    @Test fun metresAndCentimetresCoverPartialRequirements(){val r=InventoryRules.assess(listOf(Supply("cinta",D.ONE,"m")),listOf(Stock("cinta",D("60"),D.ZERO,"cm"))).single();equal("0.6",r.available);equal("0.4",r.missing);assertFalse(r.covered)}
    @Test fun kilogramsAndGramsConvert(){equal("1500",InventoryRules.convert(D("1.5"),"kg","g"));equal("0.75",InventoryRules.convert(D("750"),"g","kg"))}
    @Test fun litresAndMillilitresConvert(){equal("250",InventoryRules.convert(D("0.25"),"litros","ml"));equal("2",InventoryRules.convert(D("2000"),"ml","l"))}
    @Test fun dimensionsNeverMix(){assertNull(InventoryRules.convert(D.ONE,"g","ml"));assertNull(InventoryRules.convert(D.ONE,"m","hoja"))}
    @Test fun unknownPackageContentNeverCoversUnits(){val r=InventoryRules.assess(listOf(Supply("papel",D("5"),"hoja")),listOf(Stock("papel",D("2"),D.ZERO,"paquete"))).single();assertFalse(r.covered);assertTrue(r.review.isNotBlank())}
    @Test fun explicitPackageContentCoversSheets(){assertTrue(InventoryRules.assess(listOf(Supply("papel",D("15"),"hojas")),listOf(Stock("papel",D("2"),D.ZERO,"paquete",contentAmount=D("10"),contentUnit="hoja"))).single().covered)}
    @Test fun reservationsConvertBeforeComparing(){val r=InventoryRules.assess(listOf(Supply("cinta",D.ONE,"m")),listOf(Stock("cinta",D("200"),D("120"),"cm"))).single();equal("0.2",r.missing)}
    @Test fun duplicatesAcrossCompatibleUnitsConsolidate(){val result=InventoryRules.shopping(listOf(listOf(Supply("cinta",D.ONE,"m")),listOf(Supply("cinta",D("50"),"cm"))),emptyList());assertEquals(1,result.size);equal("1.5",result.single().quantity)}
    @Test fun incompatibleUnitsStaySeparate(){assertEquals(2,InventoryRules.shopping(listOf(listOf(Supply("papel",D.ONE,"hoja"),Supply("papel",D.ONE,"unidad"))),emptyList()).size)}
    @Test fun toolsAndReusableMaterialsAreReusedBetweenProjects(){val needs=listOf(Supply("tijeras",D.ONE,"unidad",tool=true),Supply("recipiente",D("2"),"unidad",reusable=true));val result=InventoryRules.shopping(listOf(needs,needs),emptyList());equal("1",result.first {it.tool}.quantity);equal("2",result.first {it.reusable}.quantity)}
    @Test fun consumableStockCannotCoverToolRequirements(){assertFalse(InventoryRules.assess(listOf(Supply("regla",D.ONE,"unidad",tool=true)),listOf(Stock("regla",D.ONE,D.ZERO,"unidad"))).single().covered)}
    @Test fun stableMaterialIdsMatchDocumentedSynonyms(){assertTrue(InventoryRules.assess(listOf(Supply("cartón",D.ONE,"unidad",materialId="cardboard")),listOf(Stock("cartón de caja",D.ONE,D.ZERO,"unidad",materialId="cardboard"))).single().covered)}
    @Test fun differentIdsDoNotMergeEvenWhenLabelsAreSame(){assertFalse(InventoryRules.assess(listOf(Supply("papel",D.ONE,"unidad",materialId="a")),listOf(Stock("papel",D.ONE,D.ZERO,"unidad",materialId="b"))).single().covered)}
    @Test fun safetyDoesNotDisappearWhenInventoryIsComplete(){val c=RequirementCoverage(Supply("papel",D.ONE,"hoja"),D.ONE,D.ZERO);assertTrue(WorkshopAssessment(listOf(c),SafetyStatus.ADULT).complete);assertEquals("Requiere preparación adulta",WorkshopAssessment(listOf(c),SafetyStatus.ADULT).recommendation);assertEquals("No recomendado por seguridad",WorkshopAssessment(listOf(c),SafetyStatus.FORBIDDEN).recommendation)}
    @Test fun emptyRequirementsNeverPretendAvailability(){assertFalse(WorkshopAssessment(emptyList(),SafetyStatus.REVIEWED).complete)}
    @Test fun legacyParserPreservesMalformedOriginalForRepair(){val line="madera|x|tablero|otra categoría|extra";val parsed=LegacyMaterialParser.parse(line).single();assertEquals(line,parsed.original);assertTrue(parsed.issue.isNotBlank());assertEquals("madera",parsed.name)}
    @Test fun legacyDecimalAndReusableKindParse(){val parsed=LegacyMaterialParser.parse("cinta|1,5|m|reutilizable").single();assertEquals(MaterialKind.REUSABLE,parsed.kind);equal("1.5",InventoryRules.decimal(parsed.amount));assertEquals("",parsed.issue)}
}
