package org.jpokemon.server;

import org.jpokemon.api.Overworld;
import org.jpokemon.api.PokemonTrainer;
import org.jpokemon.property.overworld.TmxFileProperties;
import org.jpokemon.property.trainer.AvatarsProperty;
import org.jpokemon.property.trainer.OverworldLocationProperty;
import org.jpokemon.server.event.PokemonTrainerLogin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zachtaylor.emissary.Emissary;
import org.zachtaylor.emissary.WebsocketConnection;
import org.zachtaylor.emissary.event.WebsocketConnectionClose;

public class OverworldEmissary extends Emissary {
	public static final OverworldEmissary instance = new OverworldEmissary();

	private OverworldEmissary() {
		register(PokemonTrainerLogin.class, this);
		register(WebsocketConnectionClose.class, this);
	}

	@Override
	public void serve(WebsocketConnection connection, JSONObject json) {
		String action = json.getString("action");

		if ("move".equals(action)) {
			move(connection, json);
		}
		else if ("look".equals(action)) {
			look(connection, json);
		}
		else if ("interact".equals(action)) {
			interact(connection, json);
		}
	}

	public void handle(PokemonTrainerLogin event) {
		PokemonTrainer pokemonTrainer = event.getPokemonTrainer();
		OverworldLocationProperty locationProperty = pokemonTrainer.getProperty(OverworldLocationProperty.class);
		AvatarsProperty avatarsProperty = pokemonTrainer.getProperty(AvatarsProperty.class);

		if (locationProperty == null) {
			locationProperty = new OverworldLocationProperty();

			// TODO - make this customizable
			locationProperty.setOverworld("bedroom");
			locationProperty.setX(10);
			locationProperty.setY(5);

			pokemonTrainer.addProperty(locationProperty);
		}
		if (avatarsProperty == null) {
			avatarsProperty = new AvatarsProperty();

			// TODO - make this customizable
			avatarsProperty.addAvailableAvatar("default");
			avatarsProperty.setAvatar("default");

			pokemonTrainer.addProperty(avatarsProperty);
		}

		Overworld overworld = Overworld.manager.getByName(locationProperty.getOverworld());

		JSONObject mapJson = new JSONObject();
		mapJson.put("event", "overworld-load-map");
		mapJson.put("mapName", locationProperty.getOverworld());
		mapJson.put("tilesets", new JSONArray(overworld.getProperty(TmxFileProperties.class).getTileSets().toString()));
		mapJson.put("entityz", overworld.getProperty(TmxFileProperties.class).getEntityZIndex());

		JSONObject playerJson = new JSONObject();
		playerJson.put("name", pokemonTrainer.getName());
		playerJson.put("avatar", avatarsProperty.getAvatar());
		playerJson.put("x", locationProperty.getX());
		playerJson.put("y", locationProperty.getY());

		JSONArray playersArray = new JSONArray();
		playersArray.put(playerJson);

		synchronized (overworld) {
			for (String otherPlayerName : overworld.getPokemonTrainers()) {
				PokemonTrainer otherPokemonTrainer = PokemonTrainer.manager.getByName(otherPlayerName);
				JSONObject otherPlayerJson = new JSONObject();
				locationProperty = otherPokemonTrainer.getProperty(OverworldLocationProperty.class);
				avatarsProperty = otherPokemonTrainer.getProperty(AvatarsProperty.class);
				otherPlayerJson.put("name", otherPlayerName);
				otherPlayerJson.put("avatar", avatarsProperty.getAvatar());
				otherPlayerJson.put("x", locationProperty.getX());
				otherPlayerJson.put("y", locationProperty.getY());

				WebsocketConnection otherPlayerConnection = PlayerRegistry.getWebsocketConnection(otherPlayerName);
				otherPlayerConnection.send(playerJson);
				playersArray.put(otherPlayerJson);
			}

			mapJson.put("players", playersArray);
			event.getConnection().send(mapJson);

			overworld.addPokemonTrainer(pokemonTrainer.getName());
		}
	}

	public void handle(WebsocketConnectionClose event) {
		String name = event.getWebsocket().getName();

		if (name == null) {
			return;
		}

		PokemonTrainer pokemonTrainer = PokemonTrainer.manager.getByName(name);
		OverworldLocationProperty locationProperty = pokemonTrainer.getProperty(OverworldLocationProperty.class);
		Overworld overworld = Overworld.manager.getByName(locationProperty.getOverworld());

		synchronized (overworld) {
			overworld.removePokemonTrainer(name);
		}
	}

	public void move(WebsocketConnection connection, JSONObject json) {
		String name = connection.getName();
		String direction = json.getString("direction");
		PokemonTrainer pokemonTrainer = PokemonTrainer.manager.getByName(name);
		OverworldLocationProperty location = pokemonTrainer.getProperty(OverworldLocationProperty.class);
		Overworld overworld = Overworld.manager.getByName(location.getOverworld());

		JSONObject updateJson = new JSONObject();
		updateJson.put("event", "overworld-move");
		updateJson.put("name", name);

		if ("up".equals(direction)) {
			location.setY(Math.max(location.getY() - 1, 0));
			updateJson.put("animation", "walkup");
		}
		else if ("left".equals(direction)) {
			location.setX(Math.max(location.getX() - 1, 0));
			updateJson.put("animation", "walkleft");
		}
		else if ("down".equals(direction)) {
			location.setY(Math.min(location.getY() + 1, overworld.getHeight() - 1));
			updateJson.put("animation", "walkdown");
		}
		else if ("right".equals(direction)) {
			location.setX(Math.min(location.getX() + 1, overworld.getWidth() - 1));
			updateJson.put("animation", "walkright");
		}

		updateJson.put("x", location.getX());
		updateJson.put("y", location.getY());

		synchronized (overworld) {
			for (String playerName : overworld.getPokemonTrainers()) {
				WebsocketConnection playerConnection = PlayerRegistry.getWebsocketConnection(playerName);
				playerConnection.send(updateJson);
			}
		}

		System.out.println("Send json object: " + updateJson.toString());
	}

	public void look(WebsocketConnection connection, JSONObject json) {

	}

	public void interact(WebsocketConnection connection, JSONObject json) {

	}
}
