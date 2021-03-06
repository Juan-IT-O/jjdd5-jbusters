package com.infoshareacademy.jbusters.web;

import com.infoshareacademy.jbusters.data.*;
import com.infoshareacademy.jbusters.freemarker.TemplateProvider;
import com.infoshareacademy.jbusters.model.User;
import com.infoshareacademy.jbusters.web.validator.NumericDataValidator;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = "/valuation")
public class ValuationServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ValuationServlet.class);

    private static final String TEMPLATE_VALUATION = "valuation";
    private static final String TEMPLATE_VALUATION_USER = "user-valuation";
    public static final String CITY = "city";
    public static final String DISTRICT_1 = "district1";
    public static final String MARKET_TYPE = "marketType";
    public static final String PRICE = "price";
    public static final String PRICE_TOTAL = "priceTotal";
    public static final String FLAT_AREA = "flatArea";
    public static final String LEVEL = "level";
    public static final String PARKING_SPOT = "parkingSpot";
    public static final String STANDARD_LEVEL = "standardLevel";
    public static final String CONSTRUCTION = "construction";
    public static final String CURRENCY_CODE = "currencyCode";

    @Inject
    private Transaction newTransaction;
    @Inject
    private TemplateProvider templateProvider;
    @Inject
    private FilterTransactions filterTransactions;
    @Inject
    private StatisticsManager statisticsManager;
    @Inject
    private CalculatePrice calc;
    @Inject
    StaticFields staticFields;


    @Inject
    private ExchangeRatesManager exchangeRatesManager;
    private NumericDataValidator numericDataValidator = new NumericDataValidator();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.addHeader("Content-Type", "text/html; charset=utf-8");
        HttpSession session = req.getSession(true);
        RequestDispatcher requestDispatcher = req.getRequestDispatcher("/load-other-values");
        Map<String, Object> model = new HashMap<>();
        Map<String, String> errorsMap = saveTransactionDetails(req);
        model.put("errors", errorsMap);

        List<Transaction> filteredList = filterTransactions.theGreatFatFilter(newTransaction);
        BigDecimal flatPriceM2 = BigDecimal.valueOf(0);
        BigDecimal flatPriceTotal = BigDecimal.valueOf(0);
        PrintWriter out = resp.getWriter();


        User sessionUser = (User) session.getAttribute("user");
        model.put("user", sessionUser);

        Template template;

        if (sessionUser == null) {
            template = templateProvider.getTemplate(
                    getServletContext(),
                    TEMPLATE_VALUATION);
        } else {
            template = templateProvider.getTemplate(
                    getServletContext(),
                    TEMPLATE_VALUATION_USER);
        }


        if (errorsMap.size() != 0) {
            try {
                req.setAttribute("errorsMap", errorsMap);
                requestDispatcher.forward(req, resp);
            } catch (ServletException e) {
                e.printStackTrace();
            }

        } else {

            if (filteredList.size() >= 11) {
                calc.setUserTransaction(newTransaction);
                calc.setFilteredList(filteredList);
                BigDecimal yearlyTrendOfPriceChange = calc.overallTrend(filteredList);
                model.put("trend", yearlyTrendOfPriceChange);

                BigDecimal minimumPriceInList = calc.getMinimumPriceInList(filteredList).setScale(2, RoundingMode.HALF_UP);
                BigDecimal averagePriceInList = calc.getAvaragePriceInList(filteredList).setScale(2, RoundingMode.HALF_UP);
                BigDecimal maxPriceInList = calc.getMaxPriceInList(filteredList).setScale(2, RoundingMode.HALF_UP);

                model.put("minimumPrice", staticFields.formatWithLongDF(minimumPriceInList));
                model.put("averagePrice", staticFields.formatWithLongDF(averagePriceInList));
                model.put("maxPrice", staticFields.formatWithLongDF(maxPriceInList));
                model.put("listTransactionUseValuation", filteredList);

                flatPriceM2 = calc.calculatePrice().setScale(2, RoundingMode.HALF_UP);
                flatPriceTotal = newTransaction.getFlatArea().multiply(flatPriceM2).setScale(2, RoundingMode.HALF_UP);

            } else {
                LOG.warn("Failed to calculate price for flat with values: {}, {}, {}, {}, {}", newTransaction.getCity(), newTransaction.getDistrict(),
                        newTransaction.getTypeOfMarket(), newTransaction.getFlatArea(), newTransaction.getConstructionYearCategory());

                if (sessionUser == null) {
                    template = templateProvider.getTemplate(getServletContext(), "no-valuation");
                } else {
                    template = templateProvider.getTemplate(getServletContext(), "user-no-valuation");
                }

            }
            session.setAttribute("priceM2", flatPriceM2);
            session.setAttribute(PRICE, flatPriceTotal);

            model.put(PRICE, staticFields.formatWithLongDF(flatPriceM2));
            model.put(PRICE_TOTAL, staticFields.formatWithLongDF(flatPriceTotal));
            model.put(CITY, newTransaction.getCity());
            model.put(DISTRICT_1, newTransaction.getDistrict());
            model.put(MARKET_TYPE, newTransaction.getTypeOfMarket());
            model.put(FLAT_AREA, newTransaction.getFlatArea());
            model.put(LEVEL, newTransaction.getLevel());
            model.put(PARKING_SPOT, newTransaction.getParkingSpot());
            model.put(STANDARD_LEVEL, newTransaction.getStandardLevel());
            model.put(CONSTRUCTION, newTransaction.getConstructionYearCategory());
            model.put(CURRENCY_CODE, exchangeRatesManager.getExCode());

            String cityName = req.getParameter("city");
            String districtName = req.getParameter("district");
            statisticsManager.captureNameFromServlet(cityName, districtName, flatPriceTotal.setScale(2, BigDecimal.ROUND_UP).toString());

            if (req.getAttribute("constructionYearError") != null) {

                if (sessionUser == null) {
                    template = templateProvider.getTemplate(
                            getServletContext(), TEMPLATE_VALUATION);
                } else {
                    template = templateProvider.getTemplate(
                            getServletContext(), TEMPLATE_VALUATION_USER);
                }

                try {
                    template.process(model, out);
                } catch (TemplateException e) {
                    LOG.error("Failed to send model due to {}", e.getMessage());
                }
            } else {
                session.setAttribute("newTransaction", newTransaction);
                try {
                    template.process(model, out);
                } catch (TemplateException e) {
                    LOG.error("Failed to send model due to {}", e.getMessage());
                }
            }
        }
    }


    private Map<String, String> saveTransactionDetails(HttpServletRequest req) {
        Map<String, String> errorsMap = new HashMap<>();

        validateData(req, errorsMap);
        validateMarketType(req, errorsMap);

        newTransaction.setFlatArea(
                numericDataValidator.validate(req.getParameter("flat-area").replaceAll(",", "."),
                        errorsMap,
                        () -> BigDecimal.valueOf(Double.parseDouble(req.getParameter("flat-area").replaceAll(",", "."))),
                        "flatAreaError",
                        "Błąd podczas zapisu wielkości mieszkania!",
                        new BigDecimal(0)));

        newTransaction.setLevel(
                numericDataValidator.validate(req.getParameter(LEVEL),
                        errorsMap,
                        () -> Integer.valueOf(req.getParameter(LEVEL)),
                        "levelError",
                        "Błąd podczas zapisu piętra!",
                        0));

        newTransaction.setConstructionYearCategory(
                numericDataValidator.validate(req.getParameter(CONSTRUCTION),
                        errorsMap,
                        () -> Integer.valueOf(req.getParameter(CONSTRUCTION)),
                        "constructionYearError", "Zła kategoria roku budowy!", 0));

        return errorsMap;
    }

    private void validateData(HttpServletRequest req, Map<String, String> errorsMap) {
        try {
            newTransaction.setTransactionDate(LocalDate.now());
            newTransaction.setCity(req.getParameter("city"));
            newTransaction.setDistrict(req.getParameter("district"));
            newTransaction.setParkingSpot(req.getParameter("parking-spot").replaceAll("_", " "));
            newTransaction.setStandardLevel(req.getParameter("standard-level").replaceAll("_", " "));
            newTransaction.setPrice(BigDecimal.valueOf(0));
            newTransaction.setPricePerM2(BigDecimal.valueOf(0));
        } catch (Exception e) {
            LOG.error("Failed to save user's due to: {}", e.getMessage());
            String errorMessage = e.getMessage();
            errorsMap.put("overallError", errorMessage);
        }
    }

    private void validateMarketType(HttpServletRequest req, Map<String, String> errorsMap) {
        String marketType = req.getParameter("market-type");
        if (marketType == null) {
            return;
        }
        if (marketType.equalsIgnoreCase("RYNEK WTÓRNY") ||
                marketType.equalsIgnoreCase("RYNEK PIERWOTNY")) {
            try {
                newTransaction.setTypeOfMarket(marketType.replaceAll("_", " "));
            } catch (Exception e) {
                LOG.error("Failed to save market type due to {}", e.getMessage());
                errorsMap.put("marketError", "Błąd podczas zapisu rodzaju rynku!");
            }
        } else {
            errorsMap.put("marketError", "Błąd podczas zapisu rodzaju rynku!");
        }
    }
}