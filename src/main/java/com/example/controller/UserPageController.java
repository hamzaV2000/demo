package com.example.controller;

import com.example.entity.*;
import com.example.services.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URL;
import java.security.Principal;
import java.sql.Timestamp;
import java.util.Set;

import static com.example.controller.Utility.getResponseContent;
import static com.example.controller.Utility.getUser;

@RestController
@RequestMapping("/profile")
public class UserPageController {
    private final String serverIP = "http://176.29.11.21";
    private User user = null;

    private final UserService userService;
    private final OwnedBookService ownedBookService;
    private final BookService bookService;
    private final ReviewService reviewService;
    private final ReviewChangesService reviewChangesService;

    private final RatingService ratingService;
    private String favorites = null;
    private Set<OwnedBook> owned;




    public UserPageController(UserService userService, OwnedBookService ownedBookService, BookService bookService, ReviewService reviewService, ReviewChangesService reviewChangesService, RatingService ratingService) {
        this.userService = userService;
        this.ownedBookService = ownedBookService;
        this.bookService = bookService;
        this.reviewService = reviewService;
        this.reviewChangesService = reviewChangesService;
        this.ratingService = ratingService;

    }

    @GetMapping("/user")
    private ResponseEntity<User> getUserInfo(Principal principal){

        User newUser = getUser(principal, userService);
        newUser.setPassword(null);
        newUser.setReviewSet(null);


        return ResponseEntity.ok().body(newUser);

    }


    @GetMapping("/favorites")
    private String getFavorites(Principal principal) throws IOException {
        if(favorites == null){
            if(user == null)
                user = getUser(principal, userService);
            URL url = new URL(serverIP + "/userFavorites/" + user.getId());
            favorites =  getResponseContent(url);
        }
        return favorites;
    }


    @GetMapping("/owned")
    private Set<OwnedBook> getOwned(Principal principal){
        if(owned == null){
            if(user == null)
                user = getUser(principal, userService);
            owned =  user.getOwnedBookSet();
        }
        return owned;
    }


    @ResponseBody
    @PostMapping("/addBookToOwned")
    private ResponseEntity<?> addBookToOwned(@RequestParam Long book_id, Principal principal){
        if(user == null)
            user = getUser(principal, userService);
        Book book = bookService.findById(book_id);
        if(book == null)
            return ResponseEntity.badRequest().body("failed");

        OwnedBook ownedBook = new OwnedBook();
        ownedBook.setBook(book);
        ownedBook.setUser(user);
        ownedBook.setAvaliable(false);
        ownedBookService.save(ownedBook);
        user.getOwnedBookSet().add(ownedBook);
        return ResponseEntity.ok("success");
    }


    @ResponseBody
    @PostMapping("/rateBook")
    private ResponseEntity<?> rateBook(@RequestParam Long book_id, @RequestParam byte rating, Principal principal){
        if(user == null)
            user = getUser(principal, userService);
        Book book = bookService.findById(book_id);
        if(book == null)
            return ResponseEntity.badRequest().body("failed");


        Review review = new Review();
        review.setBook(book);
        review.setUserRating(rating);
        review.setUser(user);
        review.setTimestamp(new Timestamp(System.currentTimeMillis()));

        ReviewChanges reviewChanges = new ReviewChanges();
        reviewChanges.setUser(user);

        reviewChangesService.save(reviewChanges);

        reviewService.save(review);


        return ResponseEntity.ok("success");
    }


    @ResponseBody
    @PostMapping("/makeBookAvailable")
    private ResponseEntity<?> makeBookAvailable(@RequestParam Long book_id , @RequestParam Boolean available, Principal principal){
        if(user == null)
            user = getUser(principal, userService);

        OwnedBook book = ownedBookService.findOwnedBookByBookAndUser(bookService.findById(book_id), user);
        book.setAvaliable(available);
        ownedBookService.save(book);

        return ResponseEntity.ok("success");
    }
}
