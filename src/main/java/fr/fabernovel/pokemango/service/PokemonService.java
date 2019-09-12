package fr.fabernovel.pokemon.service;

import java.util.*;
import java.util.stream.Collectors;

import fr.fabernovel.pokemango.model.PokemonApiResponse;
import fr.fabernovel.pokemango.model.PokemonType;
import fr.fabernovel.pokemango.model.PokemonWithStats;
import fr.fabernovel.pokemon.model.PokemonApiResponse;
import fr.fabernovel.pokemon.model.PokemonWithStats;
import fr.fabernovel.pokemon.model.PokemonType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PokemonService {

    private static final String urlApiPokeman = "https://pokeapi.co/api/v2/pokemon/";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    public PokemonService() {
    }

    /**
     * Retrieves one pokemon's details from the API.
     *
     * @param name The pokemon's name
     * @return The pokemon's details, including stats calculated from other pokemons of the same type(s).
     */
    public PokemonWithStats getPokemonByName(String name) {
        PokemonWithStats pokemonS = new PokemonWithStats();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        Map<String, String> params = new HashMap<>();
        StringBuilder urlS = new StringBuilder();
        urlS.append(urlApiPokeman);

        if (null != name && "" != name.trim()) {
            params.put("name", name);
            urlS.append("/");
            urlS.append(name);
        } else {
            urlS.append("?offset=20");
            urlS.append("&limit=20");
        }

        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

        PokemonApiResponse pokemon = restTemplate.exchange(urlS.toString(), HttpMethod.GET, entity, new ParameterizedTypeReference<PokemonApiResponse>() {
        }, params).getBody();


        if (null == pokemon) {
            return pokemonS;
        }

        setAverageStats(pokemon, restTemplate);
        setPokemon(pokemonS, pokemon);

        System.out.println(pokemonS);

        return pokemonS;
    }

    private void setAverageStats(PokemonApiResponse pokemon, RestTemplate restTemplate) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
        List<String> typesUrls = pokemon.getTypes().stream().map(type -> type.getType()).map(t -> t.getUrl()).collect(Collectors.toList());
        List<PokemonType> pokemonTypes = typesUrls.stream().map(url ->
        {
            PokemonType pokemonType = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<PokemonType>() {
            }).getBody();
            return pokemonType;
        }).collect(Collectors.toList());

        List<PokemonApiResponse> pokemons = new ArrayList<>();
        pokemonTypes.stream().
          forEach(pokemonType ->
          {
              pokemons.addAll(pokemonType.getPokemon().stream().map(pokemonL ->
                {
                    PokemonApiResponse pokemonT = restTemplate.exchange(pokemonL.getPokemon().getUrl(), HttpMethod.GET, entity, new ParameterizedTypeReference<PokemonApiResponse>() {
                    }).getBody();
                    return pokemonT;
                }
              ).collect(Collectors.toList()));

          });
        if (pokemons == null) {
            return;
        }
        pokemon.getStats().stream().forEach(pStat -> {
            List<Integer> stats = new ArrayList<>();
            //get all base_stats for the same stat
            pokemons.stream().forEach(pokemon3 ->
            {
                pokemon3.getStats().stream().forEach(pstat2 -> {
                    if (pstat2.getStat().getName().equalsIgnoreCase(pStat.getStat().getName())) {
                        stats.add(pstat2.getBase_stat());
                    }
                });
            });
            if (stats.size() != 0) {
                int averageStat = stats.stream().mapToInt(i -> i).sum() / stats.size();
                pStat.setAverageStat(averageStat);
            } else {
                pStat.setAverageStat(0);
            }
        });

    }

    private void setPokemon(PokemonWithStats pokemonS, PokemonApiResponse pokemon) {
        pokemonS.setBase_experience(pokemon.getBase_experience());
        pokemonS.setHeight(pokemon.getHeight());
        pokemonS.setId(pokemon.getId());
        pokemonS.setName(pokemon.getName());
        pokemonS.setSpecies(pokemon.getSpecies());
        pokemonS.setSprite_img(pokemon.getSprites().getFront_default());
        pokemonS.setStats(pokemon.getStats());
    }

}
