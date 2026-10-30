package pe.profelab.core.model

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal as D

class InventoryRulesTest {
    @Test fun repeatedRequirementDoesNotReuseSameConsumable(){val r=Supply("papel",D.ONE,"hojas");assertEquals(D.ONE,InventoryRules.missing(listOf(r,r),listOf(Stock("papel",D.ONE,D.ZERO,"hojas"))).single().quantity)}
    @Test fun missingQuantitiesAreReal(){val m=InventoryRules.missing(listOf(Supply("tapas",D("10"),"unidades")),listOf(Stock("tapas",D("3"),D.ZERO,"unidad")));assertEquals(D("7"),m.single().quantity)}
    @Test fun incompatibleUnitsNeverMatch(){assertEquals(1,InventoryRules.missing(listOf(Supply("agua",D("100"),"ml")),listOf(Stock("agua",D("200"),D.ZERO,"hojas"))).size)}
    @Test fun reservationsReduceAvailable(){assertEquals(D("4"),InventoryRules.missing(listOf(Supply("papel",D("10"),"hojas")),listOf(Stock("papel",D("12"),D("6"),"hojas"))).single().quantity)}
    @Test fun unknownQuantityNeverAvailable(){assertNull(InventoryRules.missing(listOf(Supply("tierra",null,"referencial: ajustar")),emptyList()).single().quantity)}
    @Test fun missingToolCounts(){assertTrue(InventoryRules.missing(listOf(Supply("tijeras",D.ONE,"unidad",true)),emptyList()).single().tool)}
    @Test fun shoppingSumsConsumablesAndReusesTools(){val needs=listOf(Supply("tapas",D("5"),"unidad"),Supply("tijeras",D.ONE,"unidad",true));val result=InventoryRules.shopping(listOf(needs,needs),emptyList());assertEquals(D("10"),result.find {it.name=="tapas"}?.quantity);assertEquals(D.ONE,result.find {it.name=="tijeras"}?.quantity)}
}
