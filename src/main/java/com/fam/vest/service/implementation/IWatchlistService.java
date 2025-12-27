package com.fam.vest.service.implementation;

import com.fam.vest.dto.response.HoldingDetails;
import com.fam.vest.dto.response.PositionDetails;
import com.fam.vest.entity.*;
import com.fam.vest.repository.*;
import com.fam.vest.service.HoldingService;
import com.fam.vest.service.PositionService;
import com.fam.vest.service.QuoteService;
import com.fam.vest.service.WatchlistService;
import com.fam.vest.dto.request.WatchlistRequest;
import com.fam.vest.enums.DEFAULT_USER_PREFERENCES;
import com.fam.vest.enums.INSTRUMENT_TOKEN;
import com.fam.vest.util.CONSTANT;
import com.zerodhatech.models.Quote;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class IWatchlistService implements WatchlistService {

    private final InstrumentRepository instrumentRepository;
    private final WatchlistRepository watchlistRepository;
    private final WatchlistInstrumentRepository watchlistInstrumentRepository;
    private final ApplicationUserRepository applicationUserRepository;
    private final HoldingService holdingService;
    private final PositionService positionService;
    private final ApplicationUserTradingAccountMappingRepository applicationUserTradingAccountMappingRepository;
    private final QuoteService quoteService;
    private final UserPreferencesRepository userPreferencesRepository;

    @Value("${fam.vest.app.maximum.watchlist.limit}")
    private int maximumWatchlistLimit;

    @Value("${fam.vest.app.watchlist.search.limit}")
    private int watchlistSearchLimit;

    @Override
    public List<Watchlist> getWatchlist(UserDetails userDetails) {
        ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(userDetails.getUsername());
        List<Watchlist> watchlist = watchlistRepository.getWatchlistByUserIdOrderById(applicationUser.getId());
        //if watchlist is empty, save configured default watchlist for the user
        if (watchlist.isEmpty()) {
            this.saveDefaultWatchlist(applicationUser);
            watchlist = watchlistRepository.getWatchlistByUserIdOrderById(applicationUser.getId());
        }
        watchlist.forEach(wl -> {
            wl.setWatchlistInstruments(watchlistInstrumentRepository.findWatchlistInstrumentByWatchlistIdOrderBySortOrder(wl.getId()));
        });
        return watchlist;
    }

    @Override
    public Watchlist updateWatchlist(UserDetails userDetails, Long watchListId,
                                     WatchlistRequest watchlistRequest) {
        this.validateWatchlistUpdateRequest(watchlistRequest);
        ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(userDetails.getUsername());
        Date currentDate = Calendar.getInstance().getTime();
        Watchlist watchlist = watchlistRepository.findWatchlistByIdAndUserId(watchListId, applicationUser.getId());
        watchlist.setName(watchlistRequest.getName());
        watchlist.setLastModifiedBy(applicationUser.getUserName());
        watchlist.setLastModifiedDate(currentDate);
        return watchlistRepository.save(watchlist);
    }

    private boolean validateWatchlistUpdateRequest(WatchlistRequest watchlistRequest) {
        if(watchlistRequest.getName().length() < 5) {
            throw new IllegalArgumentException("Watchlist name cannot be less than 5 characters");
        }
        if(watchlistRequest.getName().length() > 20) {
            throw new IllegalArgumentException("Watchlist name cannot be more than 20 characters");
        }
        return true;
    }

    @Transactional
    @Override
    public List<Watchlist> deleteWatchlistInstrument(Long watchListId, Long watchlistInstrumentId, UserDetails userDetails) {
        Watchlist watchlist = this.setWatchlistDetails(watchListId, userDetails);
        log.info("Deleting watchlist instruments {} for watchlist: {}", watchlistInstrumentId, watchlist.getName());
        watchlistInstrumentRepository.deleteByWatchlistIdAndId(watchListId, watchlistInstrumentId);
        this.saveWatchlist(watchlist);
        return this.getWatchlist(userDetails);
    }

    @Override
    public List<Watchlist> createWatchlistInstrument(Long watchListId, WatchlistInstrument watchlistInstrument, UserDetails userDetails) {
        Watchlist watchlist = this.setWatchlistDetails(watchListId, userDetails);
        log.info("Creating watchlist instruments {} for watchlist: {}", watchlistInstrument, watchlist.getName());
        // Set sortOrder to last+1
        Integer maxSortOrder = watchlistInstrumentRepository.findMaxSortOrderByWatchlistId(watchListId);
        watchlistInstrument.setSortOrder((maxSortOrder == null ? 1 : maxSortOrder + 1));
        this.saveWatchlistInstrument(watchlistInstrument);
        this.saveWatchlist(watchlist);
        return this.getWatchlist(userDetails);
    }

    @Override
    @Transactional
    public List<Watchlist> updateWatchlistInstrumentOrder(Long watchListId, List<Long> watchlistInstrumentIds, UserDetails userDetails) {
        Watchlist watchlist = this.setWatchlistDetails(watchListId, userDetails);
        for (int index = 0; index < watchlistInstrumentIds.size(); index++) {
            Long watchlistInstrumentId = watchlistInstrumentIds.get(index);
            watchlistInstrumentRepository.updateSortOrderById(watchlistInstrumentId, (index + 1));
        }
        this.saveWatchlist(watchlist);
        return this.getWatchlist(userDetails);
    }

    private Watchlist setWatchlistDetails(Long watchListId, UserDetails userDetails) {
        ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(userDetails.getUsername());
        Date currentDate = Calendar.getInstance().getTime();
        Watchlist watchlist = watchlistRepository.findWatchlistById(watchListId);
        watchlist.setLastModifiedBy(applicationUser.getUserName());
        watchlist.setLastModifiedDate(currentDate);
        return watchlist;
    }

    private void saveDefaultWatchlist(ApplicationUser applicationUser) {

        log.info("Watchlist is empty for user: {}, saving {} default watchlist", applicationUser.getUserName(), maximumWatchlistLimit);
        for (int index = 1; index <= maximumWatchlistLimit; index++) {
            this.saveWatchlist("Default Watchlist " + index, applicationUser);
        }
        log.info("{} default watchlist saved", maximumWatchlistLimit);

        log.info("Creating holdings, positions, nifty expiry and sensex expiry watchlist for user: {}", applicationUser.getUserName());
        this.saveWatchlist(CONSTANT.HOLDINGS, applicationUser);
        this.saveWatchlist(CONSTANT.POSITIONS, applicationUser);
        this.saveWatchlist(CONSTANT.NIFTY_CURRENT_WEEK_EXPIRY_WATCHLIST_NAME, applicationUser);
        this.saveWatchlist(CONSTANT.NIFTY_NEXT_WEEK_EXPIRY_WATCHLIST_NAME, applicationUser);
        this.saveWatchlist(CONSTANT.SENSEX_CURRENT_WEEK_EXPIRY_WATCHLIST_NAME, applicationUser);
        this.saveWatchlist(CONSTANT.SENSEX_NEXT_WEEK_EXPIRY_WATCHLIST_NAME, applicationUser);
        log.info("Holdings, positions, nifty expiry and sensex expiry watchlist created for user: {}", applicationUser.getUserName());
    }

    @Override
    public Watchlist saveWatchlist(String watchListName, ApplicationUser applicationUser) {
        log.info("Saving watchlist {} for {}", watchListName, applicationUser.getUserName());
        Date currentDate = Calendar.getInstance().getTime();
        Watchlist watchlist = new Watchlist();
        watchlist.setName(watchListName);
        watchlist.setUserId(applicationUser.getId());
        watchlist.setCreatedBy("System");
        watchlist.setCreatedDate(currentDate);
        watchlist.setLastModifiedBy("System");
        watchlist.setLastModifiedDate(currentDate);
        return this.saveWatchlist(watchlist);
    }

    @Override
    public WatchlistInstrument saveWatchlistInstrument(WatchlistInstrument watchlistInstrument) {
        log.info("Saving watchlist instrument {}", watchlistInstrument);
        return watchlistInstrumentRepository.save(watchlistInstrument);
    }

    @Override
    public int deleteAllWatchlistInstrumentByWatchlistId(Watchlist watchlist) {
        log.info("Deleting all watchlist instrument for {}", watchlist);
        return watchlistInstrumentRepository.deleteAllByWatchlistId(watchlist.getId());
    }

    @Override
    public Watchlist saveWatchlist(Watchlist watchlist) {
        log.info("Saving watchlist {}", watchlist);
        return watchlistRepository.save(watchlist);
    }

    @Override
    public Optional<Watchlist> findWatchlistByUserIdAndName(Long userId, String watchlistName) {
        return watchlistRepository.findWatchlistByUserIdAndName(userId, watchlistName);
    }

    @Override
    public List<Instrument> searchInstruments(String search) {
        log.info("Searching instruments with search term: {}", search);
        if (StringUtils.isBlank(search)) {
            return new ArrayList<>();
        }
        search = search.toLowerCase();
        // Normalize input: convert to lower, split and join with '&'
        String[] terms = search.trim().toLowerCase().split("\\s+");
        String tsQuery = String.join(" & ", terms);  // e.g., "nifty & may & fut"
        return instrumentRepository.searchInstrumentsWithFullText(tsQuery, watchlistSearchLimit);
    }

    private record TradingSymbolExchange(String tradingSymbol, String exchange) {}

    @Override
    public void reloadWatchlist() {
        log.info("Reloading watchlist with current and next expiry instruments");
        try {
            List<ApplicationUser> applicationUsers = applicationUserRepository.findAll();
            String niftySegment = "NFO-OPT";
            String niftyIndex = "NIFTY";
            String sensexSegment = "BFO-OPT";
            String sensexIndex = "SENSEX";
            String niftyExchangeSymbol = INSTRUMENT_TOKEN.NIFTY_50.getExchange() + ":" + INSTRUMENT_TOKEN.NIFTY_50.getSymbol();
            String sensexExchangeSymbol = INSTRUMENT_TOKEN.SENSEX.getExchange() + ":" + INSTRUMENT_TOKEN.SENSEX.getSymbol();
            Map<String, Quote> quotes = quoteService.getQuotes(new String[]{niftyExchangeSymbol, sensexExchangeSymbol});
            Quote niftyQuote = quotes.get(niftyExchangeSymbol);
            Quote sensexQuote = quotes.get(sensexExchangeSymbol);
            log.info("Reloading watchlist with nifty current week expiry instruments for: {} {}", niftySegment, niftyIndex);
            List<Instrument> niftyCurrentWeekOptionExpiryInstruments = instrumentRepository.findCurrentWeekOptionExpiryInstruments(niftySegment, niftyIndex);
            List<Instrument> filteredNiftyCurrentWeekOptionExpiryInstruments = this.filterInstrumentsBasedOnLatestPrice(niftyQuote, niftyCurrentWeekOptionExpiryInstruments);
            log.info("Total {} nifty current week expiry instruments found in local database", niftyCurrentWeekOptionExpiryInstruments.size());

            log.info("Reloading watchlist with nifty next week expiry instruments for: {} {}", niftySegment, niftyIndex);
            List<Instrument> niftyNextWeekOptionExpiryInstruments = instrumentRepository.findNextWeekOptionExpiryInstruments(niftySegment, niftyIndex);
            List<Instrument> filteredNiftyNextWeekOptionExpiryInstruments = this.filterInstrumentsBasedOnLatestPrice(niftyQuote, niftyNextWeekOptionExpiryInstruments);
            log.info("Total {} nifty next week expiry instruments found in local database", niftyNextWeekOptionExpiryInstruments.size());

            log.info("Reloading watchlist with sensex current week expiry instruments for: {} {}", sensexSegment, sensexIndex);
            List<Instrument> sensexCurrentWeekOptionExpiryInstruments = instrumentRepository.findCurrentWeekOptionExpiryInstruments(sensexSegment, sensexIndex);
            List<Instrument> filteredSensexCurrentWeekOptionExpiryInstruments  = this.filterInstrumentsBasedOnLatestPrice(sensexQuote, sensexCurrentWeekOptionExpiryInstruments);
            log.info("Total {} sensex current week expiry instruments found in local database", sensexCurrentWeekOptionExpiryInstruments.size());

            log.info("Reloading watchlist with sensex next week expiry instruments for: {} {}", sensexSegment, sensexIndex);
            List<Instrument> sensexNextWeekOptionExpiryInstruments = instrumentRepository.findNextWeekOptionExpiryInstruments(sensexSegment, sensexIndex);
            List<Instrument> filteredSensexNextWeekOptionExpiryInstruments = this.filterInstrumentsBasedOnLatestPrice(sensexQuote, sensexNextWeekOptionExpiryInstruments);
            log.info("Total {} sensex next week expiry instruments found in local database", sensexNextWeekOptionExpiryInstruments.size());
            List<HoldingDetails> holdings = holdingService.getAllHoldings();
            List<PositionDetails> positions = positionService.getAllPositions();
            applicationUsers.forEach(applicationUser -> {
                Optional<UserPreferences> userPreferences = userPreferencesRepository.getUserPreferencesByPreferenceAndUserId(DEFAULT_USER_PREFERENCES.DAILY_WATCHLIST_REFRESH, applicationUser.getId());
                if(userPreferences.isPresent() && userPreferences.get().getValue().equalsIgnoreCase("NO")) {
                    log.info("Skipping reload watchlist for user: {} as it is disabled in user preferences", applicationUser.getUserName());
                    return;
                }
                log.info("Reloading watchlist for user: {}", applicationUser.getUserName());
                List<TradingAccount> tradingAccounts = applicationUserTradingAccountMappingRepository.findTradingAccountsByApplicationUserId(applicationUser.getId());
                List<HoldingDetails>  userHoldings = holdings.stream()
                        .filter(holding -> tradingAccounts.stream()
                                .anyMatch(tradingAccount -> tradingAccount.getUserId().equals(holding.getUserId())))
                        .toList();
                log.info("Found {} holdings for user: {}", userHoldings.size(), applicationUser.getUserName());
                List<TradingSymbolExchange> holdingInstruments = userHoldings.stream().filter(h -> h.getType().equalsIgnoreCase("Stocks")).
                        map(h -> new TradingSymbolExchange(h.getTradingSymbol(), h.getExchange())).toList();
                List<Instrument> holdingsInstruments = new ArrayList<>();
                holdingInstruments.forEach(tradingSymbolExchange -> {
                    boolean isAlreadyPresent = holdingsInstruments.stream()
                            .anyMatch(instrument -> instrument.getTradingSymbol().equals(tradingSymbolExchange.tradingSymbol) &&
                                    instrument.getExchange().equals(tradingSymbolExchange.exchange));
                    if(isAlreadyPresent) {
                        return;
                    }
                    Optional<Instrument> instrument = instrumentRepository.findByTradingSymbolAndExchange(tradingSymbolExchange.tradingSymbol, tradingSymbolExchange.exchange);
                    if (instrument.isPresent()) {
                        holdingsInstruments.add(instrument.get());
                    } else {
                        log.warn("Instrument not found for trading symbol: {} and exchange: {}", tradingSymbolExchange.tradingSymbol, tradingSymbolExchange.exchange);
                    }
                });
                Optional<Watchlist> holdingsWatchlist = this.findWatchlistByUserIdAndName(applicationUser.getId(), CONSTANT.HOLDINGS);
                this.saveWatchlistInstruments(applicationUser.getUserName(), holdingsWatchlist, holdingsInstruments, CONSTANT.HOLDINGS);

                List<PositionDetails>  userPositions = positions.stream()
                        .filter(position -> tradingAccounts.stream()
                                .anyMatch(tradingAccount -> tradingAccount.getUserId().equals(position.getUserId())))
                        .toList();
                log.info("Found {} positions for user: {}", userPositions.size(), applicationUser.getUserName());
                List<TradingSymbolExchange> positionInstruments = userPositions.stream().map(position -> new TradingSymbolExchange(position.getPosition().tradingSymbol, position.getPosition().exchange)).toList();

                List<Instrument> positionsInstruments = new ArrayList<>();
                positionInstruments.forEach(tradingSymbolExchange -> {
                    boolean isAlreadyPresent = positionsInstruments.stream()
                            .anyMatch(instrument -> instrument.getTradingSymbol().equals(tradingSymbolExchange.tradingSymbol) &&
                                    instrument.getExchange().equals(tradingSymbolExchange.exchange));
                    if(isAlreadyPresent) {
                        return;
                    }
                    Optional<Instrument> instrument = instrumentRepository.findByTradingSymbolAndExchange(tradingSymbolExchange.tradingSymbol, tradingSymbolExchange.exchange);
                    if (instrument.isPresent()) {
                        positionsInstruments.add(instrument.get());
                    } else {
                        log.warn("Instrument not found for trading symbol: {} and exchange: {}", tradingSymbolExchange.tradingSymbol, tradingSymbolExchange.exchange);
                    }
                });
                Optional<Watchlist> positionsWatchlist = this.findWatchlistByUserIdAndName(applicationUser.getId(), CONSTANT.POSITIONS);
                this.saveWatchlistInstruments(applicationUser.getUserName(), positionsWatchlist, positionsInstruments, CONSTANT.POSITIONS);

                Optional<Watchlist> niftyCurrentWeekExpiryWatchlist = this.findWatchlistByUserIdAndName(applicationUser.getId(), CONSTANT.NIFTY_CURRENT_WEEK_EXPIRY_WATCHLIST_NAME);
                this.saveWatchlistInstruments(applicationUser.getUserName(), niftyCurrentWeekExpiryWatchlist, filteredNiftyCurrentWeekOptionExpiryInstruments, CONSTANT.NIFTY_CURRENT_WEEK_EXPIRY_WATCHLIST_NAME);

                Optional<Watchlist> niftyNextWeekExpiryWatchlist = this.findWatchlistByUserIdAndName(applicationUser.getId(), CONSTANT.NIFTY_NEXT_WEEK_EXPIRY_WATCHLIST_NAME);
                this.saveWatchlistInstruments(applicationUser.getUserName(), niftyNextWeekExpiryWatchlist, filteredNiftyNextWeekOptionExpiryInstruments, CONSTANT.NIFTY_NEXT_WEEK_EXPIRY_WATCHLIST_NAME);

                Optional<Watchlist> sensexCurrentWeekExpiryWatchlist = this.findWatchlistByUserIdAndName(applicationUser.getId(), CONSTANT.SENSEX_CURRENT_WEEK_EXPIRY_WATCHLIST_NAME);
                this.saveWatchlistInstruments(applicationUser.getUserName(), sensexCurrentWeekExpiryWatchlist, filteredSensexCurrentWeekOptionExpiryInstruments, CONSTANT.SENSEX_CURRENT_WEEK_EXPIRY_WATCHLIST_NAME);

                Optional<Watchlist> sensexNextWeekExpiryWatchlist = this.findWatchlistByUserIdAndName(applicationUser.getId(), CONSTANT.SENSEX_NEXT_WEEK_EXPIRY_WATCHLIST_NAME);
                this.saveWatchlistInstruments(applicationUser.getUserName(), sensexNextWeekExpiryWatchlist, filteredSensexNextWeekOptionExpiryInstruments, CONSTANT.SENSEX_NEXT_WEEK_EXPIRY_WATCHLIST_NAME);
                log.info("Reloaded watchlist for user: {}", applicationUser.getUserName());
            });
        } catch (Exception exception) {
            log.error("Exception while reloading watchlist", exception);
        }
    }

    private void saveWatchlistInstruments(String applicationUser, Optional<Watchlist> optionalWatchlist,
                                          List<Instrument> instruments, String watchlistName) {
        Date currentDate = Calendar.getInstance().getTime();
        if (optionalWatchlist.isPresent()) {
            List<WatchlistInstrument> watchlistInstruments = new ArrayList<>(instruments.size());
            Watchlist watchlist = optionalWatchlist.get();
            this.deleteAllWatchlistInstrumentByWatchlistId(watchlist);
            log.info("Saving instruments to watchlist for user: {} with name: {}", applicationUser, watchlist.getName());
            watchlist.setLastModifiedBy("System");
            watchlist.setLastModifiedDate(currentDate);

            AtomicInteger sortOrder = new AtomicInteger(1);

            instruments.forEach(instrument -> {
                // for SENSEX_CURRENT_WEEK_EXPIRY_WATCHLIST_NAME and SENSEX_NEXT_WEEK_EXPIRY_WATCHLIST_NAME if strikes are multiple of 500 then only add to watchlist
                // for NIFTY_CURRENT_WEEK_EXPIRY_WATCHLIST_NAME and NIFTY_NEXT_WEEK_EXPIRY_WATCHLIST_NAME if strikes are multiple of 100 then only add to watchlist
                Map<String, Integer> watchlistModulusMap = Map.of(
                        CONSTANT.SENSEX_CURRENT_WEEK_EXPIRY_WATCHLIST_NAME, 500,
                        CONSTANT.SENSEX_NEXT_WEEK_EXPIRY_WATCHLIST_NAME, 500,
                        CONSTANT.NIFTY_CURRENT_WEEK_EXPIRY_WATCHLIST_NAME, 100,
                        CONSTANT.NIFTY_NEXT_WEEK_EXPIRY_WATCHLIST_NAME, 100
                );

                Integer modulus = watchlistModulusMap.get(watchlistName);
                if ((modulus != null && Long.parseLong(instrument.getStrike()) % modulus == 0) ||
                        (watchlistName.equals(CONSTANT.HOLDINGS) || watchlistName.equals(CONSTANT.POSITIONS))) {
                    log.debug("Adding instrument: {} with strike: {} to watchlist: {}", instrument.getTradingSymbol(), instrument.getStrike(), watchlistName);
                    WatchlistInstrument watchlistInstrument = new WatchlistInstrument();
                    watchlistInstrument.setWatchlistId(watchlist.getId());
                    watchlistInstrument.setDisplayName(instrument.getDisplayName());
                    watchlistInstrument.setTradingSymbol(instrument.getTradingSymbol());
                    watchlistInstrument.setInstrumentToken(instrument.getInstrumentToken());
                    watchlistInstrument.setExchange(instrument.getExchange());
                    watchlistInstrument.setSegment(instrument.getSegment());
                    watchlistInstrument.setSortOrder(sortOrder.getAndIncrement());
                    watchlistInstruments.add(watchlistInstrument);
                } else {
                    log.debug("Skipping instrument: {} with strike: {} for watchlist: {} as it does not meet the criteria",
                            instrument.getTradingSymbol(), instrument.getStrike(), watchlistName);
                }
            });
            watchlistInstrumentRepository.saveAll(watchlistInstruments);
            watchlistInstrumentRepository.flush(); // flush after every batch
            this.saveWatchlist(watchlist);
        } else {
            log.info("Watchlist does not exists for user: {} with name: {}", applicationUser, watchlistName);
        }
    }

    private List<Instrument> filterInstrumentsBasedOnLatestPrice(Quote quote, List<Instrument> instruments) {
        if (quote != null) {
            log.info("Filtering instruments based on latest tick last price: {}", quote.lastPrice);
            instruments = instruments.stream().filter(optionExpiryInstrument ->
                    !((Double.parseDouble(optionExpiryInstrument.getStrike()) > quote.lastPrice && optionExpiryInstrument.getInstrumentType().equals("PE")) ||
                            (Double.parseDouble(optionExpiryInstrument.getStrike()) < quote.lastPrice && optionExpiryInstrument.getInstrumentType().equals("CE")))
            ).toList();
        } else {
            log.warn("No latestTick found");
        }
        return instruments;
    }


}
