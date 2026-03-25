package com.adventurebook.api;

import com.adventurebook.api.model.Book;
import com.adventurebook.api.model.Category;
import com.adventurebook.api.model.Consequence;
import com.adventurebook.api.model.ConsequenceType;
import com.adventurebook.api.model.Difficulty;
import com.adventurebook.api.model.Option;
import com.adventurebook.api.model.Section;
import com.adventurebook.api.model.SectionType;
import com.adventurebook.api.repository.BookRepository;
import com.adventurebook.api.repository.PlayerRepository;
import com.adventurebook.api.service.BookService;
import com.adventurebook.api.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final BookService bookService;
    private final PlayerService playerService;
    private final BookRepository bookRepository;
    private final PlayerRepository playerRepository;

    @Override
    public void run(String... args) {
        if (bookRepository.count() == 0) {
            loadCrystalCaverns();
        }
        if (playerRepository.count() == 0) {
            loadPlayers();
        }
    }

    private void loadPlayers() {
        playerService.register("Mike");
        playerService.register("Nancy");
    }

    private void loadCrystalCaverns() {
        Book book = Book.builder()
                .title("(Valid) The Crystal Caverns")
                .author("Evelyn Stormrider")
                .difficulty(Difficulty.EASY)
                .categories(Set.of(Category.FANTASY, Category.ADVENTURE))
                .sections(new ArrayList<>())
                .build();

        // Section 1 - BEGIN
        Section s1 = section(book, 1, SectionType.BEGIN,
                "You stand at the entrance of the legendary Crystal Caverns. A cold breeze carries whispers from the darkness below. A broken rope bridge leads into the depths.");
        s1.setOptions(List.of(
                option(s1, "Cross the rope bridge carefully", 100, null),
                option(s1, "Search the rocky walls for another path", 20, null)
        ));

        // Section 20
        Section s20 = section(book, 20, SectionType.NODE,
                "Your hands brush against the cold stone until you find a hidden crevice leading downward. It's dark, but you hear faint dripping water.");
        s20.setOptions(List.of(
                option(s20, "Enter the crevice", 200,
                        Consequence.builder().type(ConsequenceType.LOSE_HEALTH).value(4).text("You scrape your shoulder squeezing through the narrow gap.").build()),
                option(s20, "Return to the entrance", 1, null)
        ));

        // Section 100
        Section s100 = section(book, 100, SectionType.NODE,
                "The bridge creaks under your weight. Halfway across, a plank snaps beneath your foot!");
        s100.setOptions(List.of(
                option(s100, "Hold on tightly and crawl across", 300, null),
                option(s100, "Try to jump to the other side", 400,
                        Consequence.builder().type(ConsequenceType.LOSE_HEALTH).value(7).text("You land hard and twist your ankle.").build())
        ));

        // Section 200
        Section s200 = section(book, 200, SectionType.NODE,
                "You emerge into a glittering chamber filled with glowing crystals. At the center lies a pedestal holding an ancient gemstone.");
        s200.setOptions(List.of(
                option(s200, "Take the gemstone", 500, null),
                option(s200, "Inspect the walls carefully", 600, null)
        ));

        // Section 300
        Section s300 = section(book, 300, SectionType.NODE,
                "You crawl to safety on the other side, breathing heavily. A winding path descends deeper into the cavern.");
        s300.setOptions(List.of(
                option(s300, "Follow the path downward", 600, null),
                option(s300, "Rest and recover your strength", 700,
                        Consequence.builder().type(ConsequenceType.GAIN_HEALTH).value(3).text("You feel slightly better after resting.").build())
        ));

        // Section 400
        Section s400 = section(book, 400, SectionType.NODE,
                "You barely make the jump, but the rope bridge collapses behind you. There's no turning back now.");
        s400.setOptions(List.of(
                option(s400, "Continue deeper into the cavern", 600, null)
        ));

        // Section 500
        Section s500 = section(book, 500, SectionType.NODE,
                "The moment you touch the gemstone, the chamber begins to shake violently. A secret door opens to reveal a hidden passage.");
        s500.setOptions(List.of(
                option(s500, "Enter the hidden passage", 800, null),
                option(s500, "Drop the gemstone and run back", 200, null)
        ));

        // Section 600
        Section s600 = section(book, 600, SectionType.NODE,
                "You reach a vast underground lake glowing with bioluminescent crystals. A small boat waits at the shore.");
        s600.setOptions(List.of(
                option(s600, "Take the boat across the lake", 900, null),
                option(s600, "Swim across", 1000,
                        Consequence.builder().type(ConsequenceType.LOSE_HEALTH).value(5).text("The freezing water chills you to the bone.").build())
        ));

        // Section 700
        Section s700 = section(book, 700, SectionType.NODE,
                "After a short rest, you feel strong enough to continue your journey.");
        s700.setOptions(List.of(
                option(s700, "Head toward the underground lake", 600, null)
        ));

        // Section 800 - END
        section(book, 800, SectionType.END,
                "The hidden passage leads to a giant crystal throne room, where an ancient spirit awaits.");

        // Section 900
        Section s900 = section(book, 900, SectionType.NODE,
                "The boat carries you safely across, but you sense something massive moving in the depths below.");
        s900.setOptions(List.of(
                option(s900, "Ignore it and continue forward", 800, null),
                option(s900, "Investigate the movement", 1100, null)
        ));

        // Section 1000 - END
        section(book, 1000, SectionType.END,
                "You manage to swim across, shivering but alive. The glowing crystals ahead light your way to a massive door.");

        // Section 1100 - END
        section(book, 1100, SectionType.END,
                "You dive beneath the surface and discover an ancient underwater temple. Your adventure continues...");

        bookService.createBook(book);
    }

    private Section section(Book book, int id, SectionType type, String text) {
        Section section = Section.builder()
                .id(id)
                .type(type)
                .text(text)
                .book(book)
                .options(new ArrayList<>())
                .build();

        book.getSections().add(section);

        return section;
    }

    private Option option(Section section, String description, int gotoId, Consequence consequence) {
        return Option.builder()
                .description(description)
                .gotoId(gotoId)
                .consequence(consequence)
                .section(section)
                .build();
    }

}
