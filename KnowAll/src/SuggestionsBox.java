/*
    Copyright (C) 2004, Elliott Hughes.

    This file is part of KnowAll.

    KnowAll is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    KnowAll is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with KnowAll; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 */

import java.util.*;

public class SuggestionsBox {
    private ArrayList suggestions = new ArrayList();

    public void addSuggestion(Suggestion suggestion) {
        suggestions.add(suggestion);
    }

    public int size() {
        return suggestions.size();
    }

    public Suggestion getSuggestion(int i) {
        return (Suggestion) suggestions.get(i);
    }
}
