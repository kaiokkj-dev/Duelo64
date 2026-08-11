UPDATE chess_game_states
SET board_fen = board_fen || ' w KQkq - 0 1'
WHERE board_fen NOT LIKE '% %';

ALTER TABLE chess_game_states DROP COLUMN current_turn;
ALTER TABLE chess_game_states ALTER COLUMN board_fen TYPE VARCHAR(160);
ALTER TABLE chess_game_states ADD COLUMN position_occurrences TEXT NOT NULL DEFAULT '';
ALTER TABLE chess_game_states ADD COLUMN winner_color VARCHAR(16);
ALTER TABLE chess_game_states ADD COLUMN loser_color VARCHAR(16);
ALTER TABLE chess_game_states ADD COLUMN finish_reason VARCHAR(40);
ALTER TABLE chess_game_states ADD COLUMN finished_at TIMESTAMP WITH TIME ZONE;
