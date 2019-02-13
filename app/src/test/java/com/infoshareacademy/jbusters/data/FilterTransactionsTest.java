package com.infoshareacademy.jbusters.data;

//import org.junit.Assert;

import com.infoshareacademy.jbusters.dao.DistrictWageDao;
import com.infoshareacademy.jbusters.dao.TranzactionDao;
import com.infoshareacademy.jbusters.model.DistrictWage;
import com.infoshareacademy.jbusters.model.Tranzaction;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.mockito.internal.mockmaker.PowerMockMaker;
import org.powermock.core.PowerMockUtils;
import org.powermock.core.classloader.annotations.PowerMockListener;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.tests.utils.impl.PowerMockIgnorePackagesExtractorImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class FilterTransactionsTest {

    private static List<DistrictWage> districtWageList = new ArrayList<>();
    private static DistrictWageDao districtWageDao;
    private static List<Tranzaction> tempTranzactions = new ArrayList();
    private static List<Transaction> shortTransactions = new ArrayList();
    private static TranzactionDao tranzactionDao;
    private static StaticFields staticfields = mock(StaticFields.class);
    private static FilterTransactions testObj = new FilterTransactions();

    @BeforeClass
    public static void setDatabaseMockingValues() {

        shortTransactions.add(createTransaction(BigDecimal.TEN));

        //avg flat area list
        tempTranzactions.add(createTranzaction(new BigDecimal(6000 * 45)));

        //10 transactions around avg+-30%
        tempTranzactions.add(createTranzaction(new BigDecimal(5400 * 45)));
        tempTranzactions.add(createTranzaction(new BigDecimal(6600 * 45)));
        tempTranzactions.add(createTranzaction(new BigDecimal(4000 * 45)));
        tempTranzactions.add(createTranzaction(new BigDecimal(4100 * 45)));
        tempTranzactions.add(createTranzaction(new BigDecimal(4200 * 45)));
        tempTranzactions.add(createTranzaction(new BigDecimal(4300 * 45)));
        tempTranzactions.add(createTranzaction(new BigDecimal(7700 * 45)));
        tempTranzactions.add(createTranzaction(new BigDecimal(7800 * 45)));
        tempTranzactions.add(createTranzaction(new BigDecimal(7900 * 45)));
        tempTranzactions.add(createTranzaction(new BigDecimal(8000 * 45)));

        tranzactionDao = Mockito.mock(TranzactionDao.class);
        Mockito.when(tranzactionDao.basicFilter(anyObject(), eq("Gdynia"), anyString(), anyInt())).thenReturn(tempTranzactions);
        Mockito.when(tranzactionDao.basicFilter(anyObject(), eq("XXX"), anyString(), anyInt())).thenReturn(tempTranzactions.subList(0, 1));

        districtWageList.add(new DistrictWage("Gdynia", "Chylonia", 1));
        districtWageList.add(new DistrictWage("Gdynia", "Obłuże", 1));
        districtWageList.add(new DistrictWage("Gdynia", "Oksywie", 1));
        districtWageList.add(new DistrictWage("Gdynia", "Redłowo", 2));
        districtWageList.add(new DistrictWage("Gdynia", "Orłowo", 2));
        districtWageList.add(new DistrictWage("Gdynia", "Dąbrowa", 2));
        districtWageList.add(new DistrictWage("Sopot", "Sopot", 3));
        districtWageDao = mock(DistrictWageDao.class);
        Mockito.when(districtWageDao.findAll()).thenReturn(districtWageList);

        //given
        Whitebox.setInternalState(testObj, "tranzactionDao", tranzactionDao);
        Whitebox.setInternalState(testObj, "staticFields", staticfields);
        Whitebox.setInternalState(testObj, "districtWageDao", districtWageDao);
        Whitebox.setInternalState(testObj, "areaDiff", BigDecimal.valueOf(20.0));
        Whitebox.setInternalState(testObj, "areaDiffExpanded", BigDecimal.valueOf(25.0));
        Whitebox.setInternalState(testObj, "priceDiff", BigDecimal.valueOf(600.0));
        Whitebox.setInternalState(testObj, "minResultsNumber", 11);
        Whitebox.setInternalState(testObj, "distrWagesHandler", new DistrWagesHandler(districtWageDao));
    }

    @Test
    public void notEnoughtInitResultsShouldReturnEmptyList() {

        Transaction testTrans = createTransaction(BigDecimal.valueOf(6000));
        testTrans.setCity("XXX");
        //when
        List<Transaction> result = testObj.theGreatFatFilter(testTrans);
        //then
        assertThat(result).isEmpty();
    }

    @Test
    public void singleDistrictFilterShouldReturnListContainingOnlyUserDistrict() throws Exception {
        Transaction testTrans = createTransaction(BigDecimal.valueOf(6000));
        List<Tranzaction> testList = new ArrayList<>();
        for(int i=0;i<20;i++){
            testList.add(createTranzaction(BigDecimal.valueOf(6000)));
        }
        Tranzaction tempTranz = createTranzaction(BigDecimal.valueOf(6000));
        tempTranz.setTranzactionDistrict("Chylonia");
        testList.add(tempTranz);

        Mockito.when(tranzactionDao.basicFilter(any(LocalDate.class),anyString(),anyString(),anyInt())).thenReturn(testList);
        List<Transaction> resultList = testObj.theGreatFatFilter(testTrans);
        boolean result = resultList.stream().allMatch(t -> t.getDistrict().equals(testTrans.getDistrict()));
        assertThat(result).isTrue();

    }


//  // @Test
//    public void removeOutliersTest() {
//
//
//
//        FilterTransactions ft = new FilterTransactions();
//        tempTransactions=ft.removeOutliers(tempTransactions,new BigDecimal(600));
//
//        Assertions.assertEquals(7,tempTransactions.size());
//
//
//    }


//@Test
//    public void basicFilterTest(){
//        tempTransactions=new ArrayList();
//        LocalDate now = LocalDate.now();
//        tempTransactions.add(createTransactionByDate(now));
//        tempTransactions.add(createTransactionByDate(now.minusYears(2).plusDays(1)));
//        tempTransactions.add(createTransactionByDate(now.minusYears(2)));
//        tempTransactions.add(createTransactionByDate(now.minusYears(2).minusDays(1)));
//        tempTransactions.add(createTransactionByDate(now.minusYears(3)));
//        tempTransactions.add(createTransactionByDate(now.minusYears(4)));
//        tempTransactions.add(createTransactionByDate(now.minusYears(5)));
//        tempTransactions.add(createTransactionByDate(now.minusYears(6)));
//
//        FilterTransactions ft = new FilterTransactions();
//        ft.init();
//        tempTransactions = ft.theGreatFatFilter(createTransactionByDate(now));
//
//        Assertions.assertEquals(3,tempTransactions.size());
//
//
//    }


    private static Tranzaction createTranzaction(BigDecimal price) {

        Tranzaction trans = new Tranzaction();

        trans.setTranzactionCity("Gdynia");
        trans.setTranzactionDistrict("Witomino");
        trans.setTranzactionDataTransaction(LocalDate.now().minusMonths(2));
        trans.setTranzactionStreet("Dabrowkowska");
        trans.setTranzactionTypeOfMarket("RYNEK WTÓRNY");
        trans.setTranzactionPrice(price);
        trans.setTranzactionFlatArea(new BigDecimal(45));
        trans.setTranzactionPricePerM2(trans.getTranzactionPrice().divide(trans.getTranzactionFlatArea(), RoundingMode.HALF_UP));
        trans.setTranzactionLevel(3);
        trans.setTranzactionParkingSpot(ParkingPlace.BRAK_MP.getName());
        trans.setTranzactionStandardLevel(StandardLevel.DOBRY.getName());
        trans.setTranzactionConstructionYear("1980");
        trans.setTranzactionConstructionYearCategory(2);

        return trans;
    }

    private static Transaction createTransaction(BigDecimal price) {
        Transaction trans = new Transaction();

        Whitebox.setInternalState(trans, "exchangeRate", new BigDecimal(1));
        Whitebox.setInternalState(trans, "currency", "PLN");

        trans.setCity("Gdynia");
        trans.setDistrict("Witomino");
        trans.setTransactionDate(LocalDate.of(2018, 6, 20));
        trans.setStreet("Dabrowkowska");
        trans.setTypeOfMarket("RYNEK WTÓRNY");
        trans.setPrice(price);
        trans.setFlatArea(new BigDecimal(45));
        trans.setPricePerM2(trans.getPrice().divide(trans.getFlatArea(), RoundingMode.HALF_UP));
        trans.setLevel(3);
        trans.setParkingSpot(ParkingPlace.BRAK_MP.getName());
        trans.setStandardLevel(StandardLevel.DOBRY.getName());
        trans.setConstructionYear("1980");
        trans.setConstructionYearCategory(2);
        System.out.println(trans);
        return trans;
    }

    private Transaction createTransactionByArea(BigDecimal area) {

        Transaction trans = new Transaction();
        trans.setCity("Gdynia");
        trans.setDistrict("Witomino");
        trans.setTransactionDate(LocalDate.now().minusMonths(2));
        trans.setStreet("Dabrowkowska");
        trans.setTypeOfMarket("RYNEK WTÓRNY");
        trans.setPrice(new BigDecimal(100000));
        trans.setFlatArea(area);
        trans.setPricePerM2(trans.getPrice().divide(trans.getFlatArea(), RoundingMode.HALF_UP));
        trans.setLevel(3);
        trans.setParkingSpot(ParkingPlace.BRAK_MP.getName());
        trans.setStandardLevel(StandardLevel.DOBRY.getName());
        trans.setConstructionYear("1980");
        trans.setConstructionYearCategory(2);
        trans.setImportant(false);
        trans.setTransactionName("test");

        return trans;
    }


}